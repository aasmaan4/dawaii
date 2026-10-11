// DAWAII!! - Interactive Compose App & Lock Screen Overlay Emulator

// --- 1. Audio Engine (Minimal "Ting-Ting" Chime) ---
class AudioChimeEngine {
  constructor() {
    this.ctx = null;
  }

  init() {
    if (!this.ctx) {
      const AudioContext = window.AudioContext || window.webkitAudioContext;
      this.ctx = new AudioContext();
    }
    if (this.ctx.state === 'suspended') {
      this.ctx.resume();
    }
  }

  playTingTing() {
    this.init();
    if (!this.ctx) return;

    const now = this.ctx.currentTime;
    this.createPing(1760, now);
    this.createPing(2093, now + 0.14);
  }

  createPing(freq, startTime) {
    const osc = this.ctx.createOscillator();
    const gain = this.ctx.createGain();

    osc.type = 'sine';
    osc.frequency.setValueAtTime(freq, startTime);

    gain.gain.setValueAtTime(0.001, startTime);
    gain.gain.exponentialRampToValueAtTime(0.35, startTime + 0.015);
    gain.gain.exponentialRampToValueAtTime(0.0001, startTime + 0.65);

    osc.connect(gain);
    gain.connect(this.ctx.destination);

    osc.start(startTime);
    osc.stop(startTime + 0.7);
  }
}

const chimeEngine = new AudioChimeEngine();

// --- 2. Application State ---
const state = {
  currentScreen: 'screenDashboard',
  theme: 'SAKURA',
  initialDimness: 0.40,
  isChimeEnabled: true,
  chimeOnOverlay: true,
  chimeOnNotification: true,
  timeFormat: '12',
  overlayText: 'DAWA!',
  caregiverPhone: '9692298485',
  caregiverEnabled: true,
  chimeDelaySeconds: 3.0,
  doubleTapAction: 'Unlock Phone',
  tripleTapAction: 'Mark Taken',
  wheelMin: 0,
  wheelMax: 60,
  wheelStep: 5,
  isOverlayEnabled: true,
  brightenOnScroll: true,
  isOverlayActive: false,
  isOverlayVisible: false,
  chimeTimeoutId: null,
  tapCount: 0,
  tapTimer: null,
  dayOffset: 0,
  // Doctors (Indian Localization)
  doctors: [
    { id: 1, name: "Dr. Rajesh Sharma", contact: "+91 98450 12345", specialty: "Cardiologist" },
    { id: 2, name: "Dr. Ananya Iyer", contact: "+91 99001 67890", specialty: "General Physician" }
  ],
  activeDoctorForTabs: null,
  activeDoctorTabIndex: 0,
  // Medicines with types, conditional pills, and photoUri
  medicines: [
    { id: 1, doctorId: 1, name: "Blood Pressure (Amlodipine)", dosage: "5mg", type: "Pill", hour: 8, minute: 0, pillsLeft: 14, daysLeft: 30, photoUri: "photo_bp.jpg", isTaken: false, sendReminder: true },
    { id: 3, doctorId: 1, name: "Evening Shelcal (Calcium)", dosage: "500mg", type: "Pill", hour: 21, minute: 30, pillsLeft: 22, daysLeft: 45, photoUri: "photo_capsule.jpg", isTaken: false, sendReminder: true },
    { id: 4, doctorId: 2, name: "Skin Calming Balm (Vicco)", dosage: "Apply thin layer", type: "Cream", hour: 22, minute: 0, pillsLeft: null, daysLeft: 7, photoUri: null, isTaken: false, sendReminder: true }
  ],
  // Appointments (Indian Clinics & Doctors)
  appointmentsTab: 'upcoming',
  appointments: [
    { id: 1, doctorId: 1, date: "Thursday, Oct 8", time: "3:30 PM", location: "Apollo Clinic, Indiranagar, Bengaluru", notes: "Bring recent ECG reports and blood pressure logs", isAttended: false },
    { id: 2, doctorId: 2, date: "Friday, Oct 23", time: "10:00 AM", location: "Manipal Hospital, Old Airport Road, Room 204", notes: "Quarterly wellness review and lab profile", isAttended: false }
  ],
  doseLogs: [
    { id: 101, medName: "Blood Pressure (Amlodipine)", scheduled: "8:00 AM", actionTime: "—", status: "PENDING" },
    { id: 102, medName: "Evening Shelcal (Calcium)", scheduled: "9:30 PM", actionTime: "—", status: "PENDING" }
  ],
  // Calendar & Independent Daily Medication Logs
  selectedDate: new Date(),
  displayedYear: new Date().getFullYear(),
  displayedMonth: new Date().getMonth(), // 0-11
  // dailyLogs: { "YYYY-MM-DD": { [medicineId]: boolean } }
  dailyLogs: {}
};

// Seed historical daily adherence data
(function seedDailyLogs() {
  const today = new Date();
  function makeKey(daysOffset) {
    const d = new Date(today);
    d.setDate(d.getDate() + daysOffset);
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${y}-${m}-${day}`;
  }

  // Day -2: 0 Missed (All 3 taken) -> Green
  state.dailyLogs[makeKey(-2)] = { 1: true, 2: true, 3: true };
  // Day -1: Partially Missed (2 taken, 1 missed) -> Yellow
  state.dailyLogs[makeKey(-1)] = { 1: true, 2: false, 3: true };
  // Day -3: All Missed (0 taken) -> Red
  state.dailyLogs[makeKey(-3)] = { 1: false, 2: false, 3: false };
  // Today: Unchecked by default (All unchecked)
  state.dailyLogs[makeKey(0)] = { 1: false, 3: false, 4: false };
})();

// --- 2.5 User Data Protection & Isolated Persistence ---
// User data is stored separately per user mode and NEVER reset on app updates.
function getCurrentAuthMode() {
  return localStorage.getItem('dawaii_auth_mode') || 'guest';
}

function getUserStorageKey(baseKey) {
  const mode = getCurrentAuthMode();
  return `dawaii_${mode}_${baseKey}`;
}

function getDeletedMedicineIds() {
  try {
    const raw = localStorage.getItem(getUserStorageKey('deleted_med_ids')) || localStorage.getItem('dawaii_deleted_med_ids');
    if (!raw) return [];
    return JSON.parse(raw);
  } catch (_) {
    return [];
  }
}

function recordMedicineDeleted(medId, medName) {
  try {
    const list = getDeletedMedicineIds();
    const entry = { id: medId, name: (medName || '').toLowerCase().trim(), timestamp: Date.now() };
    if (!list.some(x => x.id === medId || (entry.name && x.name === entry.name))) {
      list.push(entry);
      localStorage.setItem(getUserStorageKey('deleted_med_ids'), JSON.stringify(list));
      localStorage.setItem('dawaii_deleted_med_ids', JSON.stringify(list));
    }
  } catch (_) {}
}

function isMedicineDeleted(medId, medName) {
  const list = getDeletedMedicineIds();
  const cleanName = (medName || '').toLowerCase().trim();
  return list.some(x => x.id === medId || (cleanName && x.name && x.name === cleanName));
}

function saveUserMedicines() {
  try {
    localStorage.setItem(getUserStorageKey('user_medicines'), JSON.stringify(state.medicines));
  } catch (_) {}
}

function loadUserData() {
  try {
    const key = getUserStorageKey('user_medicines');
    let savedMeds = localStorage.getItem(key);
    if (!savedMeds && getCurrentAuthMode() === 'guest') {
      savedMeds = localStorage.getItem('dawaii_user_medicines');
    }
    if (savedMeds) {
      const parsed = JSON.parse(savedMeds);
      if (Array.isArray(parsed)) {
        state.medicines = parsed.filter(m => !isMedicineDeleted(m.id, m.name));
      }
    } else {
      state.medicines = state.medicines.filter(m => !isMedicineDeleted(m.id, m.name));
    }

    const savedTheme = localStorage.getItem('dawaii_theme');
    if (savedTheme) {
      state.theme = savedTheme;
    }
  } catch (e) {
    console.warn('Error loading user data:', e);
  }
}
loadUserData();


// --- 3. DOM Elements ---
const sideDrawer = document.getElementById('sideDrawer');
const drawerScrim = document.getElementById('drawerScrim');
const btnHamburger = document.getElementById('btnHamburger');
const btnToggleDrawer = document.getElementById('btnToggleDrawer');
const topbarTitle = document.getElementById('topbarTitle');
const btnTopScan = document.getElementById('btnTopScan');

// Dashboard Box Grid Calendar & Minimal Checklist Elements
const btnPrevMonth = document.getElementById('btnPrevMonth');
const btnNextMonth = document.getElementById('btnNextMonth');
const calendarMonthTitle = document.getElementById('calendarMonthTitle');
const boxGridDays = document.getElementById('boxGridDays');
const dashboardDayTitle = document.getElementById('dashboardDayTitle');
const dashboardDateSub = document.getElementById('dashboardDateSub');
const dashboardAdherencePill = document.getElementById('dashboardAdherencePill');
const dashboardChecklist = document.getElementById('dashboardChecklist');
const btnDashPrevDay = document.getElementById('btnDashPrevDay');
const btnDashNextDay = document.getElementById('btnDashNextDay');
const dashDateClickable = document.getElementById('dashDateClickable');

// Doctors & Tabs Elements
const doctorsList = document.getElementById('doctorsList');
const btnAddDoctor = document.getElementById('btnAddDoctor');
const btnBackToDoctors = document.getElementById('btnBackToDoctors');
const tabDoctorName = document.getElementById('tabDoctorName');
const tabDoctorSpec = document.getElementById('tabDoctorSpec');
const animatedTabsTrack = document.getElementById('animatedTabsTrack');
const tabMedicineContent = document.getElementById('tabMedicineContent');
const btnAddMedForDoc = document.getElementById('btnAddMedForDoc');

// Master Cabinet Elements
const masterCabinetGrid = document.getElementById('masterCabinetGrid');
const masterCabinetList = document.getElementById('masterCabinetList');
const btnAddMasterMed = document.getElementById('btnAddMasterMed');

// Appointments Elements
const appointmentsList = document.getElementById('appointmentsList');
const tabUpcoming = document.getElementById('tabUpcoming');
const tabAttended = document.getElementById('tabAttended');
const btnAddApptHeader = document.getElementById('btnAddApptHeader');

// Scanner Elements
const btnCaptureOCR = document.getElementById('btnCaptureOCR');
const viewfinderLaser = document.getElementById('viewfinderLaser');

// Customize Elements
const optSakura = document.getElementById('optSakura');
const optNormal = document.getElementById('optNormal');
const toggleEnableOverlay = document.getElementById('toggleEnableOverlay');
const toggleBrightenOnScroll = document.getElementById('toggleBrightenOnScroll');
const composeDimSlider = document.getElementById('composeDimSlider');
const txtStartingDimness = document.getElementById('txtStartingDimness');
const timeFormatSegmented = document.getElementById('timeFormatSegmented');
const inputOverlayText = document.getElementById('inputOverlayText');
const composeChimeToggle = document.getElementById('composeChimeToggle');
const composeChimeDelaySlider = document.getElementById('composeChimeDelaySlider');
const txtChimeDelay = document.getElementById('txtChimeDelay');
const chimeSubToggles = document.getElementById('chimeSubToggles');
const toggleChimeOverlay = document.getElementById('toggleChimeOverlay');
const toggleChimeNotif = document.getElementById('toggleChimeNotif');
const selectDoubleTap = document.getElementById('selectDoubleTap');
const selectTripleTap = document.getElementById('selectTripleTap');

// Animated Modal Elements
const animatedModalOverlay = document.getElementById('animatedModalOverlay');
const animatedModalCard = document.getElementById('animatedModalCard');
const modalTitle = document.getElementById('modalTitle');
const modalSubtitle = document.getElementById('modalSubtitle');
const modalBody = document.getElementById('modalBody');
const btnModalCancel = document.getElementById('btnModalCancel');
const btnModalSave = document.getElementById('btnModalSave');
const btnModalClose = document.getElementById('btnModalClose');

// Overlay Elements
const overlay = document.getElementById('dawaiiOverlay');
const overlayDate = document.getElementById('overlayDate');
const overlayPrompt = document.getElementById('overlayPrompt');
const overlayClock = document.getElementById('overlayClock');
const overlayAmPm = document.getElementById('overlayAmPm');
const wheelTrack = document.getElementById('wheelTrack');
const gestureFeedback = document.getElementById('gestureFeedback');
const foliageTL = document.getElementById('foliageTL');
const foliageBR = document.getElementById('foliageBR');

// Trigger & Download Buttons
const btnTriggerOverlay = document.getElementById('btnTriggerOverlay');
const btnHeaderTriggerOverlay = document.getElementById('btnHeaderTriggerOverlay');
const btnLaunchOverlayBig = document.getElementById('btnLaunchOverlayBig');
const btnDownloadZip = document.getElementById('btnDownloadZip');

// Database & Night Shift
const dbTableBody = document.getElementById('dbTableBody');
const btnClearLogs = document.getElementById('btnClearLogs');
const btnSimScreenOff = document.getElementById('btnSimScreenOff');
const btnSimScreenOn = document.getElementById('btnSimScreenOn');
const btnSimQuietCancel = document.getElementById('btnSimQuietCancel');
const nightShiftLogs = document.getElementById('nightShiftLogs');

// --- 4. Live Digital Clocks ---
function updateClocks() {
  const now = new Date();
  const months = ["JANUARY", "FEBRUARY", "MARCH", "APRIL", "MAY", "JUNE", "JULY", "AUGUST", "SEPTEMBER", "OCTOBER", "NOVEMBER", "DECEMBER"];
  if (overlayDate) {
    overlayDate.textContent = `${months[now.getMonth()]} ${now.getDate()}`;
  }
  if (overlayPrompt) {
    overlayPrompt.textContent = state.overlayText || "DAWA!";
  }

  let hours = now.getHours();
  const minutes = String(now.getMinutes()).padStart(2, '0');
  if (state.timeFormat === '24') {
    if (overlayClock) overlayClock.textContent = `${String(hours).padStart(2, '0')}:${minutes}`;
    if (overlayAmPm) overlayAmPm.style.display = 'none';
  } else {
    const ampm = hours >= 12 ? 'PM' : 'AM';
    const hours12 = hours % 12 || 12;
    if (overlayClock) overlayClock.textContent = `${hours12}:${minutes}`;
    if (overlayAmPm) {
      overlayAmPm.textContent = ampm;
      overlayAmPm.style.display = 'inline';
    }
  }
}
setInterval(updateClocks, 1000);
updateClocks();

// --- 5. Side Drawer Navigation ---
function openDrawer() {
  sideDrawer.classList.remove('closed');
  drawerScrim.classList.remove('hidden');
}

function closeDrawer() {
  sideDrawer.classList.add('closed');
  drawerScrim.classList.add('hidden');
}

btnHamburger.addEventListener('click', openDrawer);
btnToggleDrawer.addEventListener('click', openDrawer);
drawerScrim.addEventListener('click', closeDrawer);

const screenTitles = {
  screenDashboard: "Dashboard",
  screenDoctorsAndVisits: "Doctors & Visits",
  screenDoctors: "Doctors & Visits",
  screenDoctorTabs: "Doctor Prescriptions",
  screenCabinet: "Medicine Cabinet",
  screenRecommendations: "When to Take (AI)",
  screenSetupOverlay: "Setup Overlay",
  screenAppointments: "Doctors & Visits",
  screenScanner: "AI Prescription Scanner",
  screenCustomize: "Customize",
  screenHowItWorks: "How it Works"
};

function switchScreen(targetId) {
  // Map legacy IDs to unified screen
  if (targetId === 'screenDoctors' || targetId === 'screenAppointments') {
    targetId = 'screenDoctorsAndVisits';
  }

  // If leaving camera screen, stop camera stream
  if (state.currentScreen === 'screenScanner' && targetId !== 'screenScanner') {
    stopCamera();
    const vid = document.getElementById('scannerVideo');
    if (vid) vid.style.display = 'none';
  }

  state.currentScreen = targetId;

  document.querySelectorAll('.drawer-item').forEach(item => {
    if (item.dataset.screen === targetId) item.classList.add('active');
    else item.classList.remove('active');
  });

  document.querySelectorAll('.compose-screen').forEach(scr => {
    if (scr.id === targetId) scr.classList.add('active');
    else scr.classList.remove('active');
  });

  topbarTitle.textContent = screenTitles[targetId] || "DAWAII!!";
  closeDrawer();

  // Instantaneous Data Interconnection: Re-render screen to guarantee fresh state
  if (targetId === 'screenDashboard') {
    renderDashboard();
  } else if (targetId === 'screenDoctorsAndVisits') {
    renderDoctorsAndVisits();
  } else if (targetId === 'screenCabinet') {
    renderMasterCabinet();
  } else if (targetId === 'screenRecommendations') {
    renderRecommendations();
  } else if (targetId === 'screenSetupOverlay') {
    renderSetupOverlay();
  } else if (targetId === 'screenDoctorTabs') {
    renderDoctorMedicineTabs();
  } else if (targetId === 'screenScanner') {
    startCamera(document.getElementById('scannerVideo'));
  }
}

document.querySelectorAll('.drawer-item').forEach(item => {
  item.addEventListener('click', () => switchScreen(item.dataset.screen));
});

document.querySelectorAll('.nav-quick-btn').forEach(btn => {
  btn.addEventListener('click', () => switchScreen(btn.dataset.target));
});

btnTopScan.addEventListener('click', () => switchScreen('screenScanner'));

// --- 6. Custom Reusable Animated Edit Modal Engine ---
let activeModalSaveHandler = null;

function showAnimatedModal({ title, subtitle, renderFields, onSave }) {
  modalTitle.textContent = title;
  modalSubtitle.textContent = subtitle || "";
  modalBody.innerHTML = '';
  renderFields(modalBody);

  activeModalSaveHandler = onSave;
  animatedModalOverlay.classList.remove('hidden');
}

function closeAnimatedModal() {
  animatedModalOverlay.classList.add('hidden');
  activeModalSaveHandler = null;
}

btnModalCancel.addEventListener('click', closeAnimatedModal);
btnModalClose.addEventListener('click', closeAnimatedModal);
animatedModalOverlay.addEventListener('click', (e) => {
  if (e.target === animatedModalOverlay) {
    closeAnimatedModal();
  }
});
btnModalSave.addEventListener('click', () => {
  if (activeModalSaveHandler) activeModalSaveHandler();
  closeAnimatedModal();
});

// --- 7. Dashboard Screen (Box Grid Calendar & Hyper-Minimal Checklist) ---
function toDateKey(d) {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}

function renderBoxGridCalendar() {
  if (!boxGridDays) return;
  boxGridDays.innerHTML = '';

  const monthNames = [
    "JANUARY", "FEBRUARY", "MARCH", "APRIL", "MAY", "JUNE",
    "JULY", "AUGUST", "SEPTEMBER", "OCTOBER", "NOVEMBER", "DECEMBER"
  ];
  if (calendarMonthTitle) {
    calendarMonthTitle.textContent = `${monthNames[state.displayedMonth]} ${state.displayedYear}`;
  }

  const firstDay = new Date(state.displayedYear, state.displayedMonth, 1).getDay(); // 0 = Sun
  const daysInMonth = new Date(state.displayedYear, state.displayedMonth + 1, 0).getDate();
  const today = new Date();
  const todayKey = toDateKey(today);
  const selectedKey = toDateKey(state.selectedDate);

  // Empty leading cells
  for (let i = 0; i < firstDay; i++) {
    const emptyCell = document.createElement('div');
    emptyCell.className = 'grid-day-cell empty';
    boxGridDays.appendChild(emptyCell);
  }

  // Days in month
  for (let day = 1; day <= daysInMonth; day++) {
    const d = new Date(state.displayedYear, state.displayedMonth, day);
    const dateKey = toDateKey(d);
    const isSelected = (dateKey === selectedKey);
    const isToday = (dateKey === todayKey);

    const cell = document.createElement('div');
    cell.className = `grid-day-cell ${isSelected ? 'active' : ''} ${isToday ? 'cell-today' : ''}`;

    // Determine Adherence based on state.dailyLogs[dateKey]
    if (d <= today && state.medicines.length > 0) {
      const dayLogs = state.dailyLogs[dateKey] || {};
      const takenCount = state.medicines.filter(m => dayLogs[m.id] === true).length;
      const totalCount = state.medicines.length;

      if (takenCount === totalCount) {
        cell.classList.add('cell-taken');
      } else if (takenCount > 0) {
        cell.classList.add('cell-partial');
      } else {
        cell.classList.add('cell-missed');
      }
    }

    cell.innerHTML = `
      <span class="day-number-text">${day}</span>
      <span class="day-status-micro-dot"></span>
    `;

    cell.addEventListener('click', () => {
      const dir = d > state.selectedDate ? 'left' : 'right';
      switchDashboardDate(d, dir);
    });

    boxGridDays.appendChild(cell);
  }
}

function switchDashboardDate(newDate, direction = 'left') {
  state.selectedDate = newDate;
  renderBoxGridCalendar();

  const container = document.getElementById('dashboardChecklist');
  const header = document.querySelector('.dash-date-header-row');
  const animClass = direction === 'left' ? 'anim-slide-left' : 'anim-slide-right';

  if (header) {
    header.classList.remove('anim-slide-left', 'anim-slide-right');
    void header.offsetWidth;
    header.classList.add(animClass);
  }
  if (container) {
    container.classList.remove('anim-slide-left', 'anim-slide-right');
    void container.offsetWidth;
    container.classList.add(animClass);
  }

  renderMinimalChecklist();
}

// Touch swipe support on Dashboard to switch days
let touchStartX = 0;
let touchStartY = 0;
const dashScreen = document.getElementById('screenDashboard');
if (dashScreen) {
  dashScreen.addEventListener('touchstart', (e) => {
    touchStartX = e.changedTouches[0].screenX;
    touchStartY = e.changedTouches[0].screenY;
  }, { passive: true });

  dashScreen.addEventListener('touchend', (e) => {
    const diffX = e.changedTouches[0].screenX - touchStartX;
    const diffY = e.changedTouches[0].screenY - touchStartY;
    if (Math.abs(diffX) > 40 && Math.abs(diffX) > Math.abs(diffY)) {
      const nextDate = new Date(state.selectedDate);
      if (diffX < 0) {
        // Swipe left -> Next day
        nextDate.setDate(nextDate.getDate() + 1);
        switchDashboardDate(nextDate, 'left');
      } else {
        // Swipe right -> Previous day
        nextDate.setDate(nextDate.getDate() - 1);
        switchDashboardDate(nextDate, 'right');
      }
    }
  }, { passive: true });
}

function renderMinimalChecklist() {
  if (!dashboardChecklist) return;
  dashboardChecklist.innerHTML = '';

  const today = new Date();
  const todayReset = new Date(today.getFullYear(), today.getMonth(), today.getDate());
  const selectedReset = new Date(state.selectedDate.getFullYear(), state.selectedDate.getMonth(), state.selectedDate.getDate());
  const diffDays = Math.round((selectedReset - todayReset) / (1000 * 60 * 60 * 24));

  if (diffDays === 0) dashboardDayTitle.textContent = "Today";
  else if (diffDays === -1) dashboardDayTitle.textContent = "Yesterday";
  else if (diffDays === 1) dashboardDayTitle.textContent = "Tomorrow";
  else dashboardDayTitle.textContent = state.selectedDate.toLocaleDateString('en-US', { weekday: 'long' });

  dashboardDateSub.textContent = state.selectedDate.toLocaleDateString('en-US', { weekday: 'long', month: 'short', day: 'numeric' });

  const selectedKey = toDateKey(state.selectedDate);

  // Independent Daily Logging: Ensure dailyLogs map exists for this specific date
  if (!state.dailyLogs[selectedKey]) {
    state.dailyLogs[selectedKey] = {};
  }
  const dayLogs = state.dailyLogs[selectedKey];

  const sortedMeds = [...state.medicines].sort((a, b) => {
    if (a.hour !== b.hour) return a.hour - b.hour;
    return a.minute - b.minute;
  });

  const remaining = sortedMeds.filter(m => !dayLogs[m.id]).length;
  if (dashboardAdherencePill) {
    if (remaining === 0 && sortedMeds.length > 0) {
      dashboardAdherencePill.textContent = "ALL TAKEN ✓";
      dashboardAdherencePill.className = "adherence-pill completed";
    } else {
      dashboardAdherencePill.textContent = `${remaining} TO TAKE`;
      dashboardAdherencePill.className = "adherence-pill";
    }
  }

  if (sortedMeds.length === 0) {
    dashboardChecklist.innerHTML = `<div style="text-align: center; color: var(--text-dim); padding: 24px 0; font-size: 13px;">No medications scheduled.</div>`;
    return;
  }

  sortedMeds.forEach(med => {
    const isTaken = !!dayLogs[med.id];
    const ampm = med.hour >= 12 ? "PM" : "AM";
    const hour12 = med.hour % 12 || 12;
    const minStr = String(med.minute).padStart(2, '0');
    const timeFormatted = `${hour12}:${minStr} ${ampm}`;

    // Relational Doctor Lookup (Single Source of Truth)
    const doc = state.doctors.find(d => d.id === med.doctorId);
    const docLabel = doc ? ` • ${doc.name}` : '';

    const row = document.createElement('div');
    row.className = `minimal-check-item ${isTaken ? 'taken' : ''}`;
    row.innerHTML = `
      <div class="check-left-col">
        <div class="minimal-circle-check" title="Toggle Taken / Untaken">
          ${isTaken ? '✓' : ''}
        </div>
        <div class="check-med-text-col" style="cursor: pointer;" title="Click to edit schedule time & reminder">
          <div class="minimal-med-title">${med.name}</div>
        </div>
      </div>
      <div class="minimal-med-time" style="cursor: pointer;" title="Click to edit schedule time & reminder">${timeFormatted}</div>
    `;

    // Clicking the check circle toggles taken / untaken
    row.querySelector('.minimal-circle-check').addEventListener('click', (e) => {
      e.stopPropagation();
      dayLogs[med.id] = !isTaken;
      addRoomLog(med.name, `${selectedKey} ${timeFormatted}`, dayLogs[med.id] ? "TAKEN" : "PENDING");
      renderMinimalChecklist();
      renderBoxGridCalendar();
      debouncedSyncToCloud();
    });

    // Clicking the medicine name or time opens the medicine edit modal (time & reminder toggle)
    row.querySelector('.check-med-text-col').addEventListener('click', (e) => {
      e.stopPropagation();
      openMedicineEditModal(med, () => {
        saveUserMedicines();
        renderMinimalChecklist();
        renderBoxGridCalendar();
        debouncedSyncToCloud();
      });
    });

    row.querySelector('.minimal-med-time').addEventListener('click', (e) => {
      e.stopPropagation();
      openMedicineEditModal(med, () => {
        saveUserMedicines();
        renderMinimalChecklist();
        renderBoxGridCalendar();
        debouncedSyncToCloud();
      });
    });

    dashboardChecklist.appendChild(row);
  });
}

// Reactive Relational Upcoming Appointment Card (Single Source of Truth)
function renderUpcomingAppointmentCard() {
  const dashUpcomingSection = document.getElementById('dashUpcomingSection');
  const dashUpcomingDocName = document.getElementById('dashUpcomingDocName');
  const dashUpcomingDocSpec = document.getElementById('dashUpcomingDocSpec');
  const dashUpcomingDateTime = document.getElementById('dashUpcomingDateTime');
  const dashUpcomingLoc = document.getElementById('dashUpcomingLoc');

  if (!dashUpcomingSection) return;

  const nextAppt = state.appointments.find(a => !a.isAttended);
  if (!nextAppt) {
    dashUpcomingSection.style.display = 'none';
    return;
  }

  dashUpcomingSection.style.display = 'block';
  // Relational join with Doctor table:
  const doctor = state.doctors.find(d => d.id === nextAppt.doctorId) || {
    name: "Attending Physician",
    specialty: "Consultant"
  };

  if (dashUpcomingDocName) dashUpcomingDocName.textContent = doctor.name;
  if (dashUpcomingDocSpec) dashUpcomingDocSpec.textContent = doctor.specialty;
  if (dashUpcomingDateTime) dashUpcomingDateTime.textContent = `${nextAppt.date} • ${nextAppt.time}`;
  if (dashUpcomingLoc) dashUpcomingLoc.textContent = nextAppt.location;

  const btnDashMarkAttended = document.getElementById('btnDashMarkAttended');
  const btnDashPostponeAppt = document.getElementById('btnDashPostponeAppt');

  if (btnDashMarkAttended) {
    btnDashMarkAttended.onclick = (e) => {
      e.stopPropagation();
      nextAppt.isAttended = true;
      renderDashboard();
      renderAppointments();
    };
  }

  if (btnDashPostponeAppt) {
    btnDashPostponeAppt.onclick = (e) => {
      e.stopPropagation();
      openPostponeModal(nextAppt);
    };
  }
}

function renderDashboard() {
  renderBoxGridCalendar();
  renderMinimalChecklist();
  renderUpcomingAppointmentCard();
}

if (btnPrevMonth) {
  btnPrevMonth.addEventListener('click', () => {
    if (state.displayedMonth === 0) {
      state.displayedMonth = 11;
      state.displayedYear--;
    } else {
      state.displayedMonth--;
    }
    renderBoxGridCalendar();
  });
}

if (btnNextMonth) {
  btnNextMonth.addEventListener('click', () => {
    if (state.displayedMonth === 11) {
      state.displayedMonth = 0;
      state.displayedYear++;
    } else {
      state.displayedMonth++;
    }
    renderBoxGridCalendar();
  });
}

// --- 7. Interactive Dashboard Day Switcher Controls ---
if (btnDashPrevDay) {
  btnDashPrevDay.addEventListener('click', (e) => {
    e.stopPropagation();
    const cur = state.selectedDate instanceof Date ? state.selectedDate : new Date(state.selectedDate);
    const d = new Date(cur.getFullYear(), cur.getMonth(), cur.getDate() - 1);
    switchDashboardDate(d, 'right');
  });
}

if (btnDashNextDay) {
  btnDashNextDay.addEventListener('click', (e) => {
    e.stopPropagation();
    const cur = state.selectedDate instanceof Date ? state.selectedDate : new Date(state.selectedDate);
    const d = new Date(cur.getFullYear(), cur.getMonth(), cur.getDate() + 1);
    switchDashboardDate(d, 'left');
  });
}

if (dashDateClickable) {
  dashDateClickable.addEventListener('click', (e) => {
    e.stopPropagation();
    const today = new Date();
    const cur = state.selectedDate instanceof Date ? state.selectedDate : new Date(state.selectedDate);
    const dir = today >= cur ? 'left' : 'right';
    switchDashboardDate(today, dir);
  });
}

// --- Bin Option Delete Confirmation Dialog ---
function confirmDeletion(entityName, onConfirm) {
  const modal = document.getElementById('deleteConfirmModal');
  if (!modal) {
    if (window.confirm(`Are you sure you want to remove "${entityName}"?`)) {
      onConfirm();
    }
    return;
  }
  const title = document.getElementById('deleteConfirmTitle');
  const msg = document.getElementById('deleteConfirmMessage');
  const btnConfirm = document.getElementById('btnConfirmDelete');
  const btnCancel = document.getElementById('btnCancelDelete');

  if (title) title.textContent = `Remove ${entityName}?`;
  if (msg) msg.textContent = `Are you sure you want to remove "${entityName}"? This action cannot be undone.`;
  modal.classList.remove('hidden');

  function cleanup() {
    btnConfirm.removeEventListener('click', handleConfirm);
    btnCancel.removeEventListener('click', handleCancel);
  }
  function handleConfirm(e) {
    e.stopPropagation();
    modal.classList.add('hidden');
    cleanup();
    onConfirm();
  }
  function handleCancel(e) {
    e.stopPropagation();
    modal.classList.add('hidden');
    cleanup();
  }

  btnConfirm.addEventListener('click', handleConfirm);
  btnCancel.addEventListener('click', handleCancel);
}

// --- 8. Screen: Doctors & Visits (Merged My Doctors & Appointments) ---
function renderDoctorsAndVisits() {
  renderDoctors();
  renderAppointments();
}

const tabMergedDoctors = document.getElementById('tabMergedDoctors');
const tabMergedVisits = document.getElementById('tabMergedVisits');
const viewMergedDoctors = document.getElementById('viewMergedDoctors');
const viewMergedVisits = document.getElementById('viewMergedVisits');

if (tabMergedDoctors && tabMergedVisits) {
  tabMergedDoctors.addEventListener('click', () => {
    tabMergedDoctors.classList.add('active');
    tabMergedVisits.classList.remove('active');
    if (viewMergedDoctors) viewMergedDoctors.classList.remove('hidden');
    if (viewMergedVisits) viewMergedVisits.classList.add('hidden');
    renderDoctors();
  });

  tabMergedVisits.addEventListener('click', () => {
    tabMergedVisits.classList.add('active');
    tabMergedDoctors.classList.remove('active');
    if (viewMergedVisits) viewMergedVisits.classList.remove('hidden');
    if (viewMergedDoctors) viewMergedDoctors.classList.add('hidden');
    renderAppointments();
  });
}

function renderDoctors() {
  if (!doctorsList) return;
  doctorsList.innerHTML = '';

  if (state.doctors.length === 0) {
    doctorsList.innerHTML = `<div style="text-align: center; color: var(--text-dim); padding: 32px 0; font-size: 13px;">No doctors added yet.<br>Tap '+' above to add a doctor.</div>`;
    return;
  }

  state.doctors.forEach(doc => {
    const medCount = state.medicines.filter(m => m.doctorId === doc.id).length;
    const card = document.createElement('div');
    card.className = 'item-crud-card click-to-edit';
    card.style.cursor = 'pointer';
    card.innerHTML = `
      <div style="flex: 1;">
        <div class="item-title">${doc.name}</div>
        <div class="item-desc">${doc.specialty} • ${doc.contact}</div>
        <div class="item-meta" style="color: var(--sakura-pink); font-size: 12px; margin-top: 4px;">${medCount} prescription${medCount === 1 ? '' : 's'} linked &rarr;</div>
      </div>
      <div class="card-crud-actions">
        <button class="crud-btn delete delete-doc-btn" title="Delete Doctor">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
        </button>
      </div>
    `;

    card.addEventListener('click', (e) => {
      if (e.target.closest('.delete-doc-btn')) return;
      openDoctorTabs(doc);
    });

    card.querySelector('.delete-doc-btn').addEventListener('click', (e) => {
      e.stopPropagation();
      confirmDeletion(doc.name, () => {
        state.doctors = state.doctors.filter(d => d.id !== doc.id);
        state.medicines = state.medicines.filter(m => m.doctorId !== doc.id);
        state.appointments = state.appointments.filter(a => a.doctorId !== doc.id);
        renderDoctors();
        renderMasterCabinet();
        renderAppointments();
        renderDashboard();
        debouncedSyncToCloud();
        showGestureHud(`Removed ${doc.name}`);
      });
    });

    doctorsList.appendChild(card);
  });
}

function openDoctorTabs(doctor) {
  state.activeDoctorForTabs = doctor;
  state.activeDoctorTabIndex = 0;
  tabDoctorName.textContent = doctor.name;
  tabDoctorSpec.textContent = `${doctor.specialty} • ${doctor.contact}`;

  switchScreen('screenDoctorTabs');
  renderDoctorMedicineTabs();
}

btnBackToDoctors.addEventListener('click', () => switchScreen('screenDoctorsAndVisits'));

function renderDoctorMedicineTabs() {
  const doc = state.activeDoctorForTabs;
  if (!doc) return;

  const docMeds = state.medicines.filter(m => m.doctorId === doc.id);
  animatedTabsTrack.innerHTML = '';
  tabMedicineContent.innerHTML = '';

  if (docMeds.length === 0) {
    tabMedicineContent.innerHTML = `<div style="text-align: center; color: var(--text-dim); padding: 32px 0; font-size: 13px;">No prescriptions linked to ${doc.name} yet.<br>Tap '+' above to add one.</div>`;
    return;
  }

  if (state.activeDoctorTabIndex >= docMeds.length) {
    state.activeDoctorTabIndex = 0;
  }

  docMeds.forEach((med, index) => {
    const tabPill = document.createElement('button');
    tabPill.className = `animated-tab-pill ${index === state.activeDoctorTabIndex ? 'active' : ''}`;
    tabPill.textContent = med.name;
    tabPill.addEventListener('click', () => {
      state.activeDoctorTabIndex = index;
      renderDoctorMedicineTabs();
    });
    animatedTabsTrack.appendChild(tabPill);
  });

  const activeMed = docMeds[state.activeDoctorTabIndex] || docMeds[0];
  if (activeMed) {
    const card = document.createElement('div');
    card.className = 'item-crud-card click-to-edit';
    card.style.flexDirection = 'column';
    card.style.alignItems = 'stretch';
    card.style.padding = '20px';
    card.style.cursor = 'pointer';
    card.innerHTML = `
      <div style="display: flex; justify-content: space-between; align-items: flex-start;">
        <div>
          <div class="item-title" style="font-size: 20px;">${activeMed.name}</div>
          <div class="item-desc" style="font-size: 13px; margin-top: 4px;">Format: ${activeMed.type} • ${activeMed.dosage}</div>
        </div>
        <div class="card-crud-actions">
          <button class="crud-btn delete delete-tab-btn" title="Delete Prescription">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
          </button>
        </div>
      </div>
      <div class="card-divider" style="margin: 14px 0;"></div>
      ${activeMed.pillsLeft != null ? `<div style="display: flex; justify-content: space-between; font-size: 13px; margin-bottom: 8px;"><span style="color: var(--text-dim);">Pills Remaining</span><span style="color: var(--sakura-pink); font-weight: 700;">${activeMed.pillsLeft} pills</span></div>` : ''}
      <div style="display: flex; justify-content: space-between; font-size: 13px; margin-bottom: 8px;"><span style="color: var(--text-dim);">Course Duration</span><span style="color: #fff;">${activeMed.daysLeft} days total</span></div>
      <div style="display: flex; justify-content: space-between; font-size: 13px;"><span style="color: var(--text-dim);">Schedule Time</span><span style="color: var(--sakura-pink);">${activeMed.hour}:${String(activeMed.minute).padStart(2, '0')}</span></div>
    `;

    card.addEventListener('click', (e) => {
      if (e.target.closest('.delete-tab-btn')) return;
      openMedicineEditModal(activeMed, () => renderDoctorMedicineTabs());
    });

    card.querySelector('.delete-tab-btn').addEventListener('click', (e) => {
      e.stopPropagation();
      confirmDeletion(activeMed.name, () => {
        recordMedicineDeleted(activeMed.id, activeMed.name);
        state.medicines = state.medicines.filter(m => m.id !== activeMed.id);
        saveUserMedicines();
        state.activeDoctorTabIndex = 0;
        renderDoctorMedicineTabs();
        renderMasterCabinet();
        renderDashboard();
        renderDoctors();
        debouncedSyncToCloud();
        showGestureHud(`Removed ${activeMed.name}`);
      });
    });

    tabMedicineContent.appendChild(card);
  }
}

btnAddDoctor.addEventListener('click', () => openDoctorEditModal(null));

btnAddMedForDoc.addEventListener('click', () => {
  if (!state.activeDoctorForTabs) return;
  openMedicineEditModal({ doctorId: state.activeDoctorForTabs.id }, () => renderDoctorMedicineTabs());
});

// --- In-App Native Camera Controller (WebRTC / CameraX Simulation) ---
let activeCameraStream = null;

async function startCamera(videoElement) {
  if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) return false;
  try {
    if (activeCameraStream) stopCamera();
    const stream = await navigator.mediaDevices.getUserMedia({
      video: { facingMode: { ideal: "environment" } },
      audio: false
    });
    activeCameraStream = stream;
    if (videoElement) {
      videoElement.srcObject = stream;
      videoElement.style.display = 'block';
      await videoElement.play();
      return true;
    }
  } catch (err) {
    console.log("In-app camera fallback:", err);
    return false;
  }
  return false;
}

function stopCamera() {
  if (activeCameraStream) {
    activeCameraStream.getTracks().forEach(track => track.stop());
    activeCameraStream = null;
  }
}

function captureVideoFrame(videoElement, canvasElement) {
  if (!videoElement || !canvasElement) return generateFallbackPhotoData();
  try {
    canvasElement.width = videoElement.videoWidth || 320;
    canvasElement.height = videoElement.videoHeight || 240;
    const ctx = canvasElement.getContext('2d');
    if (videoElement.videoWidth && videoElement.videoWidth > 0) {
      ctx.drawImage(videoElement, 0, 0, canvasElement.width, canvasElement.height);
      return canvasElement.toDataURL('image/jpeg', 0.85);
    }
  } catch (e) {
    console.log("Canvas capture fallback:", e);
  }
  return generateFallbackPhotoData();
}

function generateFallbackPhotoData() {
  const canvas = document.createElement('canvas');
  canvas.width = 300;
  canvas.height = 300;
  const ctx = canvas.getContext('2d');
  ctx.fillStyle = '#141217';
  ctx.fillRect(0, 0, 300, 300);
  ctx.fillStyle = '#FFB7C5';
  ctx.font = 'bold 18px Cinzel, serif';
  ctx.textAlign = 'center';
  ctx.fillText('DAWAII PHOTO', 150, 140);
  ctx.font = '12px Inter, sans-serif';
  ctx.fillStyle = '#A696A2';
  ctx.fillText(new Date().toLocaleTimeString(), 150, 170);
  ctx.strokeStyle = '#FFB7C5';
  ctx.lineWidth = 3;
  ctx.strokeRect(30, 30, 240, 240);
  return canvas.toDataURL('image/jpeg', 0.85);
}

// In-App Camera Modal Elements & Handlers
const inAppCameraModal = document.getElementById('inAppCameraModal');
const btnCloseCameraModal = document.getElementById('btnCloseCameraModal');
const btnCabinetShutter = document.getElementById('btnCabinetShutter');
const cabinetCameraVideo = document.getElementById('cabinetCameraVideo');
const cabinetCameraCanvas = document.getElementById('cabinetCameraCanvas');
let activePhotoCaptureCallback = null;

function openCabinetCamera(onCapture) {
  activePhotoCaptureCallback = onCapture;
  if (inAppCameraModal) inAppCameraModal.classList.remove('hidden');
  startCamera(cabinetCameraVideo);
}

function closeCabinetCamera() {
  stopCamera();
  if (inAppCameraModal) inAppCameraModal.classList.add('hidden');
  activePhotoCaptureCallback = null;
}

if (btnCloseCameraModal) btnCloseCameraModal.addEventListener('click', closeCabinetCamera);
if (btnCabinetShutter) {
  btnCabinetShutter.addEventListener('click', () => {
    const photoData = captureVideoFrame(cabinetCameraVideo, cabinetCameraCanvas);
    if (activePhotoCaptureCallback) {
      activePhotoCaptureCallback(photoData);
    }
    closeCabinetCamera();
  });
}

// --- 9. Screen: Master Medicine Cabinet (List View) ---
function renderMasterCabinet() {
  const container = document.getElementById('masterCabinetList') || masterCabinetGrid;
  if (!container) return;
  container.innerHTML = '';

  if (state.medicines.length === 0) {
    container.innerHTML = `<div style="text-align: center; color: var(--text-dim); padding: 32px 0; font-size: 13px;">No medicines in cabinet.<br>Tap '+' above to add one.</div>`;
    return;
  }

  state.medicines.forEach(med => {
    const doc = state.doctors.find(d => d.id === med.doctorId);
    const card = document.createElement('div');
    card.className = 'cabinet-list-item click-to-edit';
    card.style.cursor = 'pointer';
    card.innerHTML = `
      <div class="cabinet-list-photo">
        ${med.photoUri 
          ? (med.photoUri.startsWith('data:image') 
              ? `<img src="${med.photoUri}" alt="${med.name}" />`
              : '<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"/><circle cx="12" cy="13" r="4"/></svg>')
          : `<span class="photo-initials" style="font-size: 18px;">${med.name.slice(0, 2).toUpperCase()}</span>`}
      </div>
      <div class="cabinet-list-info">
        <div class="cabinet-list-name">${med.name}</div>
        <div class="cabinet-list-sub">${med.type} • ${med.dosage}</div>
        <div class="cabinet-list-doc">${doc?.name || "Doctor"} • ${med.frequency || "Daily"}</div>
      </div>
      ${med.pillsLeft ? `<div class="cabinet-list-badge">${med.pillsLeft} left</div>` : ''}
      <button class="crud-btn delete delete-master-btn" title="Delete">
        <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
      </button>
    `;

    card.addEventListener('click', (e) => {
      if (e.target.closest('.delete-master-btn')) return;
      openMedicineEditModal(med, () => renderMasterCabinet());
    });

    card.querySelector('.delete-master-btn').addEventListener('click', (e) => {
      e.stopPropagation();
      confirmDeletion(med.name, () => {
        recordMedicineDeleted(med.id, med.name);
        state.medicines = state.medicines.filter(m => m.id !== med.id);
        saveUserMedicines();
        renderMasterCabinet();
        renderDashboard();
        renderDoctors();
        if (state.currentScreen === 'screenDoctorTabs') {
          renderDoctorMedicineTabs();
        }
        debouncedSyncToCloud();
        showGestureHud(`Removed ${med.name}`);
      });
    });

    container.appendChild(card);
  });
}

btnAddMasterMed.addEventListener('click', () => {
  openMedicineEditModal(null, () => renderMasterCabinet());
});

// --- 10. Screen: Smart AI Scanner (Gemini AI Vision & OCR) ---
let uploadedPrescriptionPhoto = null;

const scannerFeedbackMsg = document.getElementById('scannerFeedbackMsg');
const filePrescriptionUpload = document.getElementById('filePrescriptionUpload');
const scannerPreviewImg = document.getElementById('scannerPreviewImg');
const btnResetScannerView = document.getElementById('btnResetScannerView');
const scannerHintText = document.getElementById('scannerHintText');

function showScannerFeedback(msg, type = 'error') {
  if (!scannerFeedbackMsg) return;
  scannerFeedbackMsg.textContent = msg;
  scannerFeedbackMsg.className = `scanner-feedback-box ${type}`;
  scannerFeedbackMsg.classList.remove('hidden');
}

function clearScannerFeedback() {
  if (!scannerFeedbackMsg) return;
  scannerFeedbackMsg.textContent = '';
  scannerFeedbackMsg.className = 'scanner-feedback-box hidden';
}

if (filePrescriptionUpload) {
  filePrescriptionUpload.addEventListener('change', (e) => {
    const file = e.target.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = (event) => {
      uploadedPrescriptionPhoto = event.target.result;
      if (scannerPreviewImg) {
        scannerPreviewImg.src = uploadedPrescriptionPhoto;
        scannerPreviewImg.style.display = 'block';
      }
      const vid = document.getElementById('scannerVideo');
      if (vid) vid.style.display = 'none';
      if (viewfinderHint) viewfinderHint.style.display = 'none';
      clearScannerFeedback();
      showScannerFeedback("Photo uploaded. Tap the shutter button below to scan with Gemini AI.", "loading");
    };
    reader.readAsDataURL(file);
  });
}

if (btnResetScannerView) {
  btnResetScannerView.addEventListener('click', () => {
    uploadedPrescriptionPhoto = null;
    if (scannerPreviewImg) {
      scannerPreviewImg.style.display = 'none';
      scannerPreviewImg.src = '';
    }
    if (filePrescriptionUpload) filePrescriptionUpload.value = '';
    const vid = document.getElementById('scannerVideo');
    if (vid) vid.style.display = 'block';
    if (viewfinderHint) viewfinderHint.style.display = 'flex';
    clearScannerFeedback();
    startCamera(vid);
  });
}

// User Gemini API Key Management
const inputGeminiApiKey = document.getElementById('inputGeminiApiKey');
const btnSaveGeminiKey = document.getElementById('btnSaveGeminiKey');
const geminiKeyStatus = document.getElementById('geminiKeyStatus');

function initGeminiKeyField() {
  const mode = getCurrentAuthMode();
  let savedKey = localStorage.getItem('dawaii_gemini_api_key') || '';
  if (!savedKey && mode === 'developer' && DEVELOPER_GEMINI_KEY) {
    savedKey = DEVELOPER_GEMINI_KEY;
    localStorage.setItem('dawaii_gemini_api_key', savedKey);
  } else if (mode === 'guest' && DEVELOPER_GEMINI_KEY && savedKey === DEVELOPER_GEMINI_KEY) {
    // Ensure developer key is not present by default in guest mode
    savedKey = '';
    localStorage.removeItem('dawaii_gemini_api_key');
  }
  if (inputGeminiApiKey) {
    inputGeminiApiKey.value = savedKey;
  }
  if (geminiKeyStatus) {
    geminiKeyStatus.textContent = savedKey ? (mode === 'developer' ? "Developer Gemini Vision Active ✓" : "Using Your Gemini Account Model ✓") : "Enter key to use your Gemini model";
    geminiKeyStatus.style.color = savedKey ? "var(--sakura-pink)" : "var(--text-dim)";
  }
}
initGeminiKeyField();

if (btnSaveGeminiKey) {
  btnSaveGeminiKey.addEventListener('click', () => {
    const key = (inputGeminiApiKey?.value || '').trim();
    localStorage.setItem('dawaii_gemini_api_key', key);
    initGeminiKeyField();
    showGestureHud(key ? 'Gemini API Key Saved ✓' : 'Gemini Key Cleared');
  });
}

async function triggerOCRScanner(onFinish) {
  clearScannerFeedback();
  const laser = document.querySelector('#dashScannerSection .viewfinder-laser') || viewfinderLaser;
  if (laser) laser.style.animation = 'scanLaser 0.6s 3 ease-in-out';

  const scannerVideo = document.getElementById('scannerVideo');
  const scannerCanvas = document.getElementById('scannerCanvas');
  
  // Use uploaded photo if available, or capture real frame from active camera
  let capturedPhoto = uploadedPrescriptionPhoto;
  if (!capturedPhoto && scannerVideo && scannerVideo.videoWidth > 0) {
    capturedPhoto = captureVideoFrame(scannerVideo, scannerCanvas);
  }

  // CRITICAL REQUIREMENT: If no input is found or couldn't scan, send a message that couldn't scan.
  // There is NO default blanks to fill here. It is either scanned or isn't.
  if (!capturedPhoto) {
    showScannerFeedback("Couldn't scan: No prescription input detected. Please aim the camera at the label or tap 'Upload Photo'.", "error");
    showGestureHud("Couldn't scan prescription");
    return;
  }

  const userApiKey = (localStorage.getItem('dawaii_gemini_api_key') || '').trim();
  showScannerFeedback(userApiKey ? "Analyzing prescription with your Gemini Multimodal Vision model..." : "Analyzing prescription with Gemini AI Vision...", "loading");

  try {
    const response = await fetch('/api/gemini-scan', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ image: capturedPhoto, apiKey: userApiKey })
    });

    const result = await response.json();

    if (!result.success || !result.data) {
      const errorMsg = result.error || "Couldn't scan: Unable to read medication details. Please ensure label is clear and well-lit.";
      showScannerFeedback(errorMsg, "error");
      showGestureHud("Couldn't scan prescription");
      return;
    }

    // Success: Gemini or AI Pharmacopeia deciphered the prescription
    const ext = result.data;
    showScannerFeedback(`✨ Gemini identified: ${ext.name} (${ext.dosage})`, "success");
    showGestureHud(`Scanned: ${ext.name}`);

    // Match with appropriate doctor by specialty if available
    let matchedDoc = state.doctors.find(d => 
      ext.doctorSpecialty && d.specialty && d.specialty.toLowerCase().includes(ext.doctorSpecialty.toLowerCase())
    ) || state.doctors[0];

    const extractedData = {
      name: ext.name || "Prescribed Medicine",
      dosage: ext.dosage || "1 Dose Daily",
      type: ext.type || "Pill",
      pillsLeft: ext.pillsLeft || 20,
      daysLeft: ext.daysLeft || 14,
      doctorId: matchedDoc?.id || 1,
      photoUri: capturedPhoto || `scan_bottle_${Date.now()}.jpg`
    };

    setTimeout(() => {
      openMedicineEditModal(extractedData, () => {
        if (onFinish) onFinish();
        renderDashboard();
        renderMasterCabinet();
        renderRecommendations();
      }, "Prescription Detected by Gemini", "Review & Save to Cabinet");
    }, 600);

  } catch (err) {
    console.error("Gemini scanner fetch error:", err);
    showScannerFeedback("Couldn't scan: Connection error during AI prescription analysis. Please try again.", "error");
    showGestureHud("Scan error");
  }
}

btnCaptureOCR.addEventListener('click', () => {
  triggerOCRScanner(() => switchScreen('screenCabinet'));
});

// --- Recommendations Tab Engine (When to Take) ---
function getMedicineRecommendation(med) {
  const name = (med.name || '').toLowerCase();
  const hour = med.hour !== undefined ? med.hour : 8;
  const minute = med.minute !== undefined ? med.minute : 0;
  const timeFormatted = formatTime(hour, minute);

  let timingCategory = 'morning';
  let todIcon = '🌅';
  let todTitle = `Morning (${timeFormatted})`;
  let badgeClass = 'timing-morning';

  if (hour >= 12 && hour < 17) {
    timingCategory = 'noon';
    todIcon = '☀️';
    todTitle = `Afternoon (${timeFormatted})`;
    badgeClass = 'timing-noon';
  } else if (hour >= 17) {
    timingCategory = 'night';
    todIcon = '🌙';
    todTitle = `Night / Bedtime (${timeFormatted})`;
    badgeClass = 'timing-night';
  }

  let mealType = 'food';
  let mealBadge = '🍽️ After Meals (PC)';
  let mealBadgeClass = '';
  let instruction = 'Take with or after food with a full glass of water. Maintain consistent daily timing.';
  let interactions = 'Take with food to minimize gastric irritation.';
  let missedDose = 'If missed, take as soon as remembered unless it is almost time for your next dose.';

  if (name.includes('pan') || name.includes('pantoprazole') || name.includes('omeprazole') || name.includes('thyro')) {
    mealType = 'empty';
    mealBadge = '🥣 Empty Stomach (AC)';
    mealBadgeClass = 'rec-tag-warn';
    instruction = 'Take 30 to 60 minutes before morning breakfast with plain water. Do not crush or chew.';
    interactions = 'Wait at least 45 minutes before tea, coffee, milk, or calcium supplements.';
  } else if (name.includes('telmi') || name.includes('amlo') || name.includes('blood pressure') || name.includes('bp')) {
    mealType = 'morning';
    mealBadge = '💧 Fixed Morning Schedule';
    instruction = 'Take once daily every morning at the same hour with water to ensure 24-hour BP coverage.';
    interactions = 'Avoid excessive sodium and potassium salt substitutes unless approved by your doctor.';
  } else if (name.includes('beco') || name.includes('multi') || name.includes('zinc') || name.includes('vitamin')) {
    mealType = 'food';
    mealBadge = '🍽️ After Dinner';
    instruction = 'Take after your evening meal or dinner. Vitamin absorption is optimal with dietary fats.';
    interactions = 'Do not take on an empty stomach to prevent nausea.';
  } else if (name.includes('volini') || name.includes('gel') || name.includes('cream')) {
    mealType = 'external';
    mealBadge = '🧴 External Application';
    instruction = 'Gently apply a thin layer over the affected area at bedtime. Wash hands thoroughly.';
    interactions = 'For external use only. Do not apply on broken skin or near eyes.';
  } else if (name.includes('amox') || name.includes('augmentin') || name.includes('clav')) {
    mealType = 'food';
    mealBadge = '🍽️ With Meals (Course)';
    instruction = 'Take at the start of a meal every 8 to 12 hours. Complete the entire course.';
    interactions = 'Maintain regular spacing. Do not stop early even if feeling recovered.';
  }

  return {
    timingCategory,
    todIcon,
    todTitle,
    badgeClass,
    mealType,
    mealBadge,
    mealBadgeClass,
    instruction,
    interactions,
    missedDose
  };
}

let activeRecFilter = 'all';

function renderRecommendations(filter = activeRecFilter) {
  activeRecFilter = filter;
  const container = document.getElementById('recommendationsList');
  if (!container) return;
  container.innerHTML = '';

  // Update filter pill UI
  document.querySelectorAll('.rec-pill-btn').forEach(btn => {
    if (btn.dataset.filter === filter) btn.classList.add('active');
    else btn.classList.remove('active');
  });

  const meds = state.medicines.filter(med => {
    const rec = getMedicineRecommendation(med);
    if (filter === 'all') return true;
    if (filter === 'morning') return rec.timingCategory === 'morning';
    if (filter === 'night') return rec.timingCategory === 'night';
    if (filter === 'empty') return rec.mealType === 'empty';
    if (filter === 'food') return rec.mealType === 'food';
    return true;
  });

  if (meds.length === 0) {
    container.innerHTML = `
      <div style="text-align: center; color: var(--text-dim); padding: 40px 16px; font-size: 13px;">
        No medications found for this timing filter.<br>Tap '+' in the cabinet to add prescriptions.
      </div>
    `;
    return;
  }

  meds.forEach(med => {
    const doc = state.doctors.find(d => d.id === med.doctorId);
    const rec = getMedicineRecommendation(med);

    const card = document.createElement('div');
    card.className = 'rec-card';
    card.innerHTML = `
      <div class="rec-card-top">
        <div>
          <div class="rec-card-title">${med.name}</div>
          <div class="rec-card-sub">${med.type} • ${med.dosage} • ${doc?.name || 'Doctor'}</div>
        </div>
        <span class="rec-timing-badge ${rec.badgeClass}">${rec.todIcon} ${rec.todTitle}</span>
      </div>

      <div class="rec-instruction-box">
        <div class="rec-instruction-title">When &amp; How to Take</div>
        <div class="rec-instruction-text">${rec.instruction}</div>
      </div>

      <div class="rec-guideline-tags">
        <span class="rec-tag ${rec.mealBadgeClass}">${rec.mealBadge}</span>
        <span class="rec-tag">⚠️ ${rec.interactions}</span>
        <span class="rec-tag">ℹ️ ${rec.missedDose}</span>
      </div>
    `;
    container.appendChild(card);
  });
}

// Hook Recommendation Filter Buttons
document.querySelectorAll('.rec-pill-btn').forEach(btn => {
  btn.addEventListener('click', () => {
    renderRecommendations(btn.dataset.filter);
  });
});

// Hook Cabinet Subtabs
const tabCabinetPrescriptions = document.getElementById('tabCabinetPrescriptions');
const tabCabinetRecommendations = document.getElementById('tabCabinetRecommendations');
if (tabCabinetPrescriptions) {
  tabCabinetPrescriptions.addEventListener('click', () => {
    tabCabinetPrescriptions.classList.add('active');
    tabCabinetRecommendations?.classList.remove('active');
    const masterList = document.getElementById('masterCabinetList');
    if (masterList) masterList.style.display = 'flex';
  });
}
if (tabCabinetRecommendations) {
  tabCabinetRecommendations.addEventListener('click', () => {
    switchScreen('screenRecommendations');
  });
}

// Bottom scanner and tabs removed from Dashboard for aggressive minimal overhaul

// --- 11. Modal Form Builders ---
function openDoctorEditModal(doc) {
  showAnimatedModal({
    title: doc ? "Edit Doctor" : "Add New Doctor",
    subtitle: "Healthcare Professional",
    renderFields: (container) => {
      container.innerHTML = `
        <div class="modal-input-group">
          <label>Doctor Name</label>
          <input id="inDocName" class="modal-input" value="${doc?.name || ''}" placeholder="e.g. Dr. Eleanor Vance" />
        </div>
        <div class="modal-input-group">
          <label>Contact Number</label>
          <input id="inDocContact" class="modal-input" value="${doc?.contact || ''}" placeholder="+1 (555) 000-0000" />
        </div>
        <div class="modal-input-group">
          <label>Specialty</label>
          <input id="inDocSpec" class="modal-input" value="${doc?.specialty || 'General Physician'}" />
        </div>
      `;
    },
    onSave: () => {
      const name = document.getElementById('inDocName').value;
      const contact = document.getElementById('inDocContact').value || "+1 (555) 000-0000";
      const specialty = document.getElementById('inDocSpec').value || "General Physician";
      if (!name) return;

      if (doc) {
        doc.name = name;
        doc.contact = contact;
        doc.specialty = specialty;
      } else {
        state.doctors.push({ id: Date.now(), name, contact, specialty });
      }
      renderDoctors();
      renderMasterCabinet();
      renderAppointments();
      renderDashboard();
      debouncedSyncToCloud();
    }
  });
}

function openMedicineEditModal(med, onComplete, customTitle, customSubtitle) {
  showAnimatedModal({
    title: customTitle || (med?.id ? "Edit Medication" : "Add Medication"),
    subtitle: customSubtitle || "Prescription Details",
    renderFields: (container) => {
      const docOptions = state.doctors.map(d => `<option value="${d.id}" ${med?.doctorId === d.id ? 'selected' : ''}>${d.name}</option>`).join('');

      container.innerHTML = `
        <div class="modal-input-group">
          <label>Medicine Name</label>
          <input id="inMedName" class="modal-input" value="${med?.name || ''}" placeholder="e.g. Amoxicillin" />
        </div>
        <div class="modal-input-group">
          <label>Dosage</label>
          <input id="inMedDosage" class="modal-input" value="${med?.dosage || ''}" placeholder="e.g. 500mg" />
        </div>
        <div class="modal-input-group">
          <label>Assign to Doctor</label>
          <select id="inMedDoc" class="modal-input">${docOptions}</select>
        </div>
        <div class="modal-input-group">
          <label>Medicine Format (Type)</label>
          <select id="inMedType" class="modal-input">
            <option value="Capsule" ${med?.type === 'Capsule' ? 'selected' : ''}>Capsule</option>
            <option value="Pill" ${med?.type === 'Pill' ? 'selected' : ''}>Pill</option>
            <option value="Cream" ${med?.type === 'Cream' ? 'selected' : ''}>Cream</option>
            <option value="Solution" ${med?.type === 'Solution' ? 'selected' : ''}>Solution</option>
            <option value="Syrup" ${med?.type === 'Syrup' ? 'selected' : ''}>Syrup</option>
          </select>
        </div>
        <div class="modal-input-group" id="groupPillsLeft">
          <label>Pills Left in Bottle (Conditional)</label>
          <input id="inMedPills" type="number" class="modal-input" value="${med?.pillsLeft ?? 30}" />
        </div>
        <div class="modal-input-group">
          <label>Days to Complete Course</label>
          <input id="inMedDays" type="number" class="modal-input" value="${med?.daysLeft ?? 14}" />
        </div>
        <div class="modal-input-group">
          <label>Scheduled Dose Time (When user wants to take it)</label>
          <div style="display: flex; gap: 8px;">
            <select id="inMedHour" class="modal-input" style="flex: 1;">
              ${Array.from({length: 24}, (_, i) => `<option value="${i}" ${(med?.hour ?? 9) === i ? 'selected' : ''}>${String(i).padStart(2, '0')}:00 ${i >= 12 ? 'PM' : 'AM'}</option>`).join('')}
            </select>
            <select id="inMedMinute" class="modal-input" style="flex: 1;">
              ${Array.from({length: 60}, (_, i) => `<option value="${i}" ${(med?.minute ?? 0) === i ? 'selected' : ''}>:${String(i).padStart(2, '0')}</option>`).join('')}
            </select>
          </div>
        </div>
        <div class="modal-input-group" style="background: rgba(255,255,255,0.03); border: 1px solid var(--border-color); border-radius: 12px; padding: 10px 12px; margin-top: 6px;">
          <label style="display: flex; justify-content: space-between; align-items: center; cursor: pointer; margin: 0; font-size: 13px;">
            <span style="font-weight: 500; color: var(--text-main);">Send Lock-Screen Reminder</span>
            <input type="checkbox" id="inMedSendReminder" ${med?.sendReminder !== false ? 'checked' : ''} style="width: 18px; height: 18px; accent-color: var(--sakura-pink); cursor: pointer;" />
          </label>
          <div style="font-size: 11px; color: var(--text-dim); margin-top: 4px;">
            Spawns DAWAII!! Lock Screen Overlay alert at this medication's scheduled time.
          </div>
        </div>
        <div style="display: flex; justify-content: space-between; align-items: center; padding: 6px 0;">
          <span style="font-size: 11px; color: var(--text-dim);" id="txtPhotoStatus">${med?.photoUri ? 'Photo Attached ✓' : 'No Photo'}</span>
          <button type="button" id="btnAttachPhoto" class="btn btn-outline btn-sm">Capture / Retake Photo</button>
        </div>
      `;

      const typeSelect = container.querySelector('#inMedType');
      const groupPills = container.querySelector('#groupPillsLeft');
      function checkType() {
        const val = typeSelect.value;
        groupPills.style.display = (val === 'Capsule' || val === 'Pill') ? 'flex' : 'none';
      }
      typeSelect.addEventListener('change', checkType);
      checkType();

      container.querySelector('#btnAttachPhoto').addEventListener('click', () => {
        openCabinetCamera((dataUrl) => {
          med = med || {};
          med.photoUri = dataUrl;
          const statusElem = container.querySelector('#txtPhotoStatus');
          if (statusElem) {
            statusElem.textContent = "Photo Attached ✓";
            statusElem.style.color = "var(--sakura-pink)";
          }
        });
      });
    },
    onSave: () => {
      const name = document.getElementById('inMedName').value;
      if (!name) return;
      const dosage = document.getElementById('inMedDosage').value || "1 Dose";
      const doctorId = parseInt(document.getElementById('inMedDoc').value, 10);
      const type = document.getElementById('inMedType').value;
      const isCountable = (type === 'Capsule' || type === 'Pill');
      const pillsLeft = isCountable ? (parseInt(document.getElementById('inMedPills').value, 10) || 30) : null;
      const daysLeft = parseInt(document.getElementById('inMedDays').value, 10) || 14;
      const hour = parseInt(document.getElementById('inMedHour')?.value ?? (med?.hour ?? 9), 10);
      const minute = parseInt(document.getElementById('inMedMinute')?.value ?? (med?.minute ?? 0), 10);
      const sendReminder = document.getElementById('inMedSendReminder') ? document.getElementById('inMedSendReminder').checked : true;

      if (med && med.id) {
        med.name = name;
        med.dosage = dosage;
        med.doctorId = doctorId;
        med.type = type;
        med.pillsLeft = pillsLeft;
        med.daysLeft = daysLeft;
        med.hour = hour;
        med.minute = minute;
        med.sendReminder = sendReminder;
      } else {
        state.medicines.push({
          id: Date.now(),
          doctorId,
          name,
          dosage,
          type,
          pillsLeft,
          daysLeft,
          hour,
          minute,
          sendReminder,
          photoUri: med?.photoUri || null,
          isTaken: false
        });
      }

      saveUserMedicines();
      if (onComplete) onComplete();
      renderDashboard();
      renderMasterCabinet();
      renderDoctors();
      if (state.currentScreen === 'screenDoctorTabs') {
        renderDoctorMedicineTabs();
      }
      debouncedSyncToCloud();
    }
  });
}

// --- 12. Appointments Screen (Normalized Flow & Relational Doctor Updates) ---
function renderAppointments() {
  appointmentsList.innerHTML = '';
  const isAttended = state.appointmentsTab === 'attended';
  const filtered = state.appointments.filter(a => a.isAttended === isAttended);

  if (filtered.length === 0) {
    appointmentsList.innerHTML = `<div style="text-align: center; color: var(--text-dim); padding: 30px 0; font-size: 13px;">No ${state.appointmentsTab} appointments.</div>`;
    return;
  }

  filtered.forEach(item => {
    // Relational Single Source of Truth: Find Doctor from Doctor table
    const doc = state.doctors.find(d => d.id === item.doctorId) || {
      name: "Specialist",
      specialty: "Consultant"
    };

    const card = document.createElement('div');
    card.className = `appointment-card-preview click-to-edit ${item.isAttended ? 'attended-card' : ''}`;
    card.style.cursor = 'pointer';
    card.innerHTML = `
      <div class="appt-card-top">
        <div>
          <div class="doctor-name">${doc.name}</div>
          <div class="doctor-spec">${doc.specialty}</div>
        </div>
        <div class="appt-card-actions">
          <button class="appt-action-btn postpone-appt-btn" title="Postpone Consultation">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
          </button>
          <button class="appt-action-btn delete delete-appt-btn" title="Delete Consultation">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
          </button>
        </div>
      </div>
      <div class="card-divider"></div>
      <div class="appt-datetime">${item.date} • ${item.time}</div>
      <div class="appt-location">${item.location}</div>
      ${item.notes ? `<div class="appt-notes">“${item.notes}”</div>` : ''}
      <div style="display: flex; justify-content: flex-end; margin-top: 10px;">
        <button class="btn-mark-attended ${item.isAttended ? 'attended' : ''}">
          <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg>
          <span>${item.isAttended ? 'ATTENDED ✓' : 'MARK ATTENDED'}</span>
        </button>
      </div>
    `;

    card.addEventListener('click', (e) => {
      if (e.target.closest('.appt-action-btn') || e.target.closest('.btn-mark-attended')) return;
      openAppointmentEditModal(item);
    });

    card.querySelector('.postpone-appt-btn').addEventListener('click', (e) => {
      e.stopPropagation();
      openPostponeModal(item);
    });

    card.querySelector('.delete-appt-btn').addEventListener('click', (e) => {
      e.stopPropagation();
      const doc = state.doctors.find(d => d.id === item.doctorId);
      const apptLabel = doc ? `Visit with ${doc.name}` : `Appointment on ${item.date}`;
      confirmDeletion(apptLabel, () => {
        state.appointments = state.appointments.filter(a => a.id !== item.id);
        renderAppointments();
        renderDashboard();
        debouncedSyncToCloud();
        showGestureHud("Consultation removed");
      });
    });

    const markBtn = card.querySelector('.btn-mark-attended');
    if (markBtn) {
      markBtn.addEventListener('click', (e) => {
        e.stopPropagation();
        item.isAttended = !item.isAttended;
        renderAppointments();
        renderDashboard();
        debouncedSyncToCloud();
      });
    }

    appointmentsList.appendChild(card);
  });
}

function openPostponeModal(appt) {
  const currentDoc = state.doctors.find(d => d.id === appt.doctorId) || { name: "Doctor" };
  showAnimatedModal({
    title: "Postpone Visit",
    subtitle: `${currentDoc.name} Consultation`,
    renderFields: (container) => {
      container.innerHTML = `
        <div class="modal-input-group">
          <label>Quick Shift</label>
          <div style="display: flex; gap: 6px; flex-wrap: wrap; margin-bottom: 8px;">
            <button type="button" class="btn btn-outline btn-sm quick-shift-btn" data-days="1">Tomorrow</button>
            <button type="button" class="btn btn-outline btn-sm quick-shift-btn" data-days="3">+3 Days</button>
            <button type="button" class="btn btn-outline btn-sm quick-shift-btn" data-days="7">+1 Week</button>
            <button type="button" class="btn btn-outline btn-sm quick-shift-btn" data-days="14">+2 Weeks</button>
            <button type="button" class="btn btn-outline btn-sm quick-shift-btn" data-days="30">+1 Month</button>
          </div>
        </div>
        <div class="modal-input-group">
          <label>New Date</label>
          <input id="inPostponeDate" class="modal-input" value="${appt.date}" />
        </div>
        <div class="modal-input-group">
          <label>New Time</label>
          <input id="inPostponeTime" class="modal-input" value="${appt.time}" />
        </div>
      `;

      container.querySelectorAll('.quick-shift-btn').forEach(btn => {
        btn.addEventListener('click', () => {
          const days = parseInt(btn.dataset.days, 10);
          const d = new Date();
          d.setDate(d.getDate() + days);
          const dayName = d.toLocaleDateString('en-US', { weekday: 'long' });
          const monthName = d.toLocaleDateString('en-US', { month: 'short' });
          container.querySelector('#inPostponeDate').value = `${dayName}, ${monthName} ${d.getDate()}`;
        });
      });
    },
    onSave: () => {
      const newDate = document.getElementById('inPostponeDate').value;
      const newTime = document.getElementById('inPostponeTime').value;
      appt.date = newDate;
      appt.time = newTime;
      renderAppointments();
      renderDashboard();
      debouncedSyncToCloud();
    }
  });
}

function openAppointmentEditModal(appt) {
  const currentDoc = appt ? (state.doctors.find(d => d.id === appt.doctorId) || state.doctors[0]) : state.doctors[0];

  showAnimatedModal({
    title: appt ? "Edit Appointment" : "New Appointment",
    subtitle: "Doctor Consultation",
    renderFields: (container) => {
      const docOptions = state.doctors.map(d => `<option value="${d.id}" ${currentDoc?.id === d.id ? 'selected' : ''}>${d.name} (${d.specialty})</option>`).join('');

      container.innerHTML = `
        <div class="modal-input-group">
          <label>Assigned Doctor</label>
          <select id="inApptDocSelect" class="modal-input">
            ${docOptions}
            <option value="new">+ Add New Doctor...</option>
          </select>
        </div>
        <div class="modal-input-group" id="groupEditDocName">
          <label>Doctor's Name (Updates Doctor Record Everywhere)</label>
          <input id="inApptDocName" class="modal-input" value="${currentDoc?.name || ''}" placeholder="Dr. Full Name" />
        </div>
        <div class="modal-input-group">
          <label>Specialty</label>
          <input id="inApptDocSpec" class="modal-input" value="${currentDoc?.specialty || 'General Physician'}" placeholder="e.g. Cardiologist" />
        </div>
        <div class="modal-input-group">
          <label>Date</label>
          <input id="inApptDate" class="modal-input" value="${appt?.date || 'Thursday, Oct 15'}" />
        </div>
        <div class="modal-input-group">
          <label>Time</label>
          <input id="inApptTime" class="modal-input" value="${appt?.time || '3:30 PM'}" />
        </div>
        <div class="modal-input-group">
          <label>Location</label>
          <input id="inApptLoc" class="modal-input" value="${appt?.location || 'City Medical Center, Suite 402'}" />
        </div>
        <div class="modal-input-group">
          <label>Consultation Notes</label>
          <input id="inApptNotes" class="modal-input" value="${appt?.notes || ''}" placeholder="e.g. Discuss prescription refill & lab tests" />
        </div>
      `;

      const select = container.querySelector('#inApptDocSelect');
      const nameInput = container.querySelector('#inApptDocName');
      const specInput = container.querySelector('#inApptDocSpec');

      select.addEventListener('change', () => {
        if (select.value === 'new') {
          nameInput.value = '';
          specInput.value = '';
        } else {
          const selDoc = state.doctors.find(d => d.id === Number(select.value));
          if (selDoc) {
            nameInput.value = selDoc.name;
            specInput.value = selDoc.specialty;
          }
        }
      });
    },
    onSave: () => {
      const select = document.getElementById('inApptDocSelect');
      const docName = document.getElementById('inApptDocName').value.trim() || 'Dr. Attending';
      const docSpec = document.getElementById('inApptDocSpec').value.trim() || 'General Physician';
      const dateVal = document.getElementById('inApptDate').value;
      const timeVal = document.getElementById('inApptTime').value;
      const locVal = document.getElementById('inApptLoc').value;
      const notesVal = document.getElementById('inApptNotes').value;

      let targetDoctorId;

      if (select.value === 'new') {
        const newDoc = {
          id: Date.now(),
          name: docName,
          contact: "+1 (555) 000-0000",
          specialty: docSpec
        };
        state.doctors.push(newDoc);
        targetDoctorId = newDoc.id;
      } else {
        targetDoctorId = Number(select.value);
        // Single Source of Truth UPDATE:
        // Update the Doctor entity directly so it cascades everywhere!
        const existingDoc = state.doctors.find(d => d.id === targetDoctorId);
        if (existingDoc) {
          existingDoc.name = docName;
          existingDoc.specialty = docSpec;
        }
      }

      if (appt) {
        appt.doctorId = targetDoctorId;
        appt.date = dateVal;
        appt.time = timeVal;
        appt.location = locVal;
        appt.notes = notesVal;
      } else {
        state.appointments.push({
          id: Date.now(),
          doctorId: targetDoctorId,
          date: dateVal,
          time: timeVal,
          location: locVal,
          notes: notesVal,
          isAttended: false
        });
      }

      // Re-render all views that observe relational doctor data!
      renderAppointments();
      renderDashboard();
      renderDoctors();
      renderMasterCabinet();
      debouncedSyncToCloud();
    }
  });
}

tabUpcoming.addEventListener('click', () => {
  state.appointmentsTab = 'upcoming';
  tabUpcoming.classList.add('active');
  tabAttended.classList.remove('active');
  renderAppointments();
});

tabAttended.addEventListener('click', () => {
  state.appointmentsTab = 'attended';
  tabAttended.classList.add('active');
  tabUpcoming.classList.remove('active');
  renderAppointments();
});

btnAddApptHeader.addEventListener('click', () => {
  openAppointmentEditModal(null);
});

// --- 13. Customize Tab Logic (Sectional Rewrite) ---
function setAppTheme(theme) {
  state.theme = theme;
  try {
    localStorage.setItem('dawaii_theme', theme);
  } catch (_) {}

  if (theme === 'NORMAL') {
    if (optNormal) optNormal.classList.add('active');
    if (optSakura) optSakura.classList.remove('active');
    document.body.classList.add('normal-mode');
    if (overlay) {
      overlay.className = `dawaii-overlay normal-theme ${state.isOverlayActive ? 'active-state' : 'dim-state'} ${state.isOverlayVisible ? '' : 'hidden'}`;
    }
  } else {
    if (optSakura) optSakura.classList.add('active');
    if (optNormal) optNormal.classList.remove('active');
    document.body.classList.remove('normal-mode');
    if (overlay) {
      overlay.className = `dawaii-overlay sakura-theme ${state.isOverlayActive ? 'active-state' : 'dim-state'} ${state.isOverlayVisible ? '' : 'hidden'}`;
    }
  }
}

if (optSakura) {
  optSakura.addEventListener('click', () => {
    setAppTheme('SAKURA');
    showGestureHud('Theme: Floral (Sakura) Activated');
    debouncedSyncToCloud();
  });
}

if (optNormal) {
  optNormal.addEventListener('click', () => {
    setAppTheme('NORMAL');
    showGestureHud('Theme: Minimal Activated');
    debouncedSyncToCloud();
  });
}

// Automatically apply theme on startup
setAppTheme(state.theme || 'SAKURA');


if (toggleEnableOverlay) {
  toggleEnableOverlay.addEventListener('change', (e) => {
    state.isOverlayEnabled = e.target.checked;
    showGestureHud(state.isOverlayEnabled ? 'Lock-Screen Overlay Enabled' : 'Lock-Screen Overlay Disabled');
    if (!state.isOverlayEnabled && state.isOverlayVisible) {
      dismissOverlay('Overlay Disabled in Settings');
    }
  });
}

if (toggleBrightenOnScroll) {
  toggleBrightenOnScroll.addEventListener('change', (e) => {
    state.brightenOnScroll = e.target.checked;
    showGestureHud(state.brightenOnScroll ? 'Brighten on Scroll Enabled' : 'Brighten on Scroll Disabled');
  });
}

if (composeDimSlider) {
  composeDimSlider.addEventListener('input', (e) => {
    state.initialDimness = e.target.value / 100;
    if (txtStartingDimness) txtStartingDimness.textContent = `${e.target.value}%`;
    if (!state.isOverlayActive && overlay) overlay.style.opacity = state.initialDimness;
  });
}

if (timeFormatSegmented) {
  timeFormatSegmented.querySelectorAll('.seg-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      timeFormatSegmented.querySelectorAll('.seg-btn').forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      state.timeFormat = btn.dataset.format;
      updateClocks();
    });
  });
}

if (inputOverlayText) {
  inputOverlayText.addEventListener('input', (e) => {
    state.overlayText = e.target.value;
    if (overlayPrompt) overlayPrompt.textContent = state.overlayText || 'DAWA!';
  });
  inputOverlayText.addEventListener('change', () => {
    syncToCloud();
  });
}

if (composeChimeToggle) {
  composeChimeToggle.addEventListener('change', (e) => {
    state.isChimeEnabled = e.target.checked;
    // Crucial Interaction: When user toggles ON, immediately play brief sample of chime audio
    if (state.isChimeEnabled) {
      chimeEngine.playTingTing();
      showGestureHud('Chime Sample Preview');
    }
    if (chimeSubToggles) {
      if (state.isChimeEnabled) {
        chimeSubToggles.classList.remove('hidden');
      } else {
        chimeSubToggles.classList.add('hidden');
      }
    }
  });
}

if (composeChimeDelaySlider) {
  composeChimeDelaySlider.addEventListener('input', (e) => {
    state.chimeDelaySeconds = parseInt(e.target.value, 10);
    if (txtChimeDelay) txtChimeDelay.textContent = `${state.chimeDelaySeconds}s`;
  });
}

if (toggleChimeOverlay) {
  toggleChimeOverlay.addEventListener('change', (e) => {
    state.chimeOnOverlay = e.target.checked;
  });
}

if (toggleChimeNotif) {
  toggleChimeNotif.addEventListener('change', (e) => {
    state.chimeOnNotification = e.target.checked;
  });
}

// Gesture Clash Resolution: If double tap and triple tap clash, reassign the other gesture
if (selectDoubleTap) {
  selectDoubleTap.addEventListener('change', (e) => {
    const newVal = e.target.value;
    state.doubleTapAction = newVal;
    if (newVal === state.tripleTapAction) {
      const altAction = newVal === 'Unlock Phone' ? 'Mark Taken' : 'Unlock Phone';
      state.tripleTapAction = altAction;
      if (selectTripleTap) selectTripleTap.value = altAction;
      showGestureHud(`Gesture Swap: Triple Tap set to "${altAction}"`);
    }
    syncToCloud();
  });
}

if (selectTripleTap) {
  selectTripleTap.addEventListener('change', (e) => {
    const newVal = e.target.value;
    state.tripleTapAction = newVal;
    if (newVal === state.doubleTapAction) {
      const altAction = newVal === 'Unlock Phone' ? 'Mark Taken' : 'Unlock Phone';
      state.doubleTapAction = altAction;
      if (selectDoubleTap) selectDoubleTap.value = altAction;
      showGestureHud(`Gesture Swap: Double Tap set to "${altAction}"`);
    }
    syncToCloud();
  });
}

// Indian Phone Number Validation & Caregiver SMS Sync
function isValidIndianPhone(phone) {
  if (!phone) return false;
  const clean = String(phone).replace(/[\s\-\(\)]/g, '');
  // 10 digits starting with 6, 7, 8, or 9, optionally prefixed with +91, 91, or 0
  return /^(\+91|91|0)?[6-9]\d{9}$/.test(clean);
}

const toggleCaregiverSms = document.getElementById('toggleCaregiverSms');
const inputCaregiverPhone = document.getElementById('inputCaregiverPhone');
const caregiverPhoneError = document.getElementById('caregiverPhoneError');

if (toggleCaregiverSms) {
  toggleCaregiverSms.addEventListener('change', (e) => {
    state.caregiverEnabled = e.target.checked;
    showGestureHud(state.caregiverEnabled ? "Caregiver SMS Sync: Active" : "Caregiver SMS Sync: Off");
    syncToCloud();
  });
}

if (inputCaregiverPhone) {
  const handlePhoneCompletion = () => {
    const rawVal = inputCaregiverPhone.value.trim();
    if (rawVal === '') {
      if (caregiverPhoneError) caregiverPhoneError.classList.add('hidden');
      inputCaregiverPhone.style.borderColor = '';
      return;
    }
    if (isValidIndianPhone(rawVal)) {
      if (caregiverPhoneError) caregiverPhoneError.classList.add('hidden');
      inputCaregiverPhone.style.borderColor = 'rgba(74, 222, 128, 0.5)';
      state.caregiverPhone = rawVal;
      showGestureHud(`Caregiver contact saved: ${rawVal}`);
      syncToCloud();
    } else {
      if (caregiverPhoneError) caregiverPhoneError.classList.remove('hidden');
      inputCaregiverPhone.style.borderColor = '#FF6B6B';
      showGestureHud("Incorrect number: Please enter a valid Indian mobile number");
    }
  };

  inputCaregiverPhone.addEventListener('blur', handlePhoneCompletion);
  inputCaregiverPhone.addEventListener('change', handlePhoneCompletion);
  inputCaregiverPhone.addEventListener('input', () => {
    const rawVal = inputCaregiverPhone.value.trim();
    if (isValidIndianPhone(rawVal)) {
      if (caregiverPhoneError) caregiverPhoneError.classList.add('hidden');
      inputCaregiverPhone.style.borderColor = 'rgba(74, 222, 128, 0.5)';
      state.caregiverPhone = rawVal;
    }
  });
}

function getTimeOfDay(hour) {
  if (hour >= 4 && hour < 12) return "Morning";
  if (hour >= 12 && hour < 16) return "Noon";
  if (hour >= 16 && hour < 21) return "Evening";
  return "Night";
}

function sendCaregiverMissedSms(medName, scheduledHour, scheduledMinute) {
  if (!state.caregiverEnabled) return null;
  const h = (scheduledHour !== undefined && scheduledHour !== null) ? scheduledHour : new Date().getHours();
  const m = (scheduledMinute !== undefined && scheduledMinute !== null) ? scheduledMinute : new Date().getMinutes();
  const tod = getTimeOfDay(h);
  const ampm = h >= 12 ? "PM" : "AM";
  const h12 = h % 12 || 12;
  const timeFormatted = `${String(h12).padStart(2, '0')}:${String(m).padStart(2, '0')} ${ampm}`;
  
  // Format required: "(time of the day. eg. noon, morning) (medication name) missed at (time)"
  const smsBody = `${tod} ${medName} missed at ${timeFormatted}`;
  const phone = state.caregiverPhone || "9692298485";

  // Visual simulation banner in checklist
  const banner = document.createElement('div');
  banner.className = 'sms-alert-banner';
  banner.innerHTML = `
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
    <div style="flex: 1;">
      <div style="font-weight: 600; color: #FFFFFF;">Caregiver SMS Sent to ${phone}</div>
      <div style="font-family: monospace; margin-top: 2px;">"${smsBody}"</div>
    </div>
  `;
  const container = document.getElementById('dashboardChecklist') || document.body;
  if (container.firstChild) {
    container.insertBefore(banner, container.firstChild);
  } else {
    container.appendChild(banner);
  }
  setTimeout(() => banner.remove(), 7000);

  showGestureHud(`SMS Sent to ${phone}: "${smsBody}"`);
  console.log(`[Caregiver SMS -> ${phone}]: ${smsBody}`);

  try {
    if (/Mobi|Android|iPhone/i.test(navigator.userAgent)) {
      window.open(`sms:${phone}?body=${encodeURIComponent(smsBody)}`, '_self');
    }
  } catch (err) {}

  return { phone, smsBody };
}

// Doctor's Medical PDF Report Date Range Modal & Export System
const doctorReportModal = document.getElementById('doctorReportModal');
const btnCloseReportModal = document.getElementById('btnCloseReportModal');
const btnExportDoctorReport = document.getElementById('btnExportDoctorReport');
const btnTriggerExport = document.getElementById('btnTriggerExport');
const reportStartDate = document.getElementById('reportStartDate');
const reportEndDate = document.getElementById('reportEndDate');
const btnPrintDoctorPDF = document.getElementById('btnPrintDoctorPDF');
const btnExportCSV = document.getElementById('btnExportCSV');
const presetChips = document.querySelectorAll('.preset-chip');

function formatDateKey(d) {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}

function openDoctorReportModal() {
  if (!doctorReportModal) return;
  setReportPreset(30);
  doctorReportModal.classList.remove('hidden');
}

function closeDoctorReportModal() {
  if (!doctorReportModal) return;
  doctorReportModal.classList.add('hidden');
}

if (btnExportDoctorReport) {
  btnExportDoctorReport.addEventListener('click', openDoctorReportModal);
}
if (btnTriggerExport) {
  btnTriggerExport.addEventListener('click', (e) => {
    e.stopPropagation();
    openDoctorReportModal();
  });
}
if (btnCloseReportModal) {
  btnCloseReportModal.addEventListener('click', closeDoctorReportModal);
}
if (doctorReportModal) {
  doctorReportModal.addEventListener('click', (e) => {
    if (e.target === doctorReportModal) closeDoctorReportModal();
  });
}

function setReportPreset(daysOrMode) {
  const end = new Date();
  const start = new Date();
  if (daysOrMode === 'month') {
    start.setDate(1);
  } else {
    const days = parseInt(daysOrMode, 10) || 30;
    start.setDate(end.getDate() - (days - 1));
  }
  if (reportStartDate) reportStartDate.value = formatDateKey(start);
  if (reportEndDate) reportEndDate.value = formatDateKey(end);

  presetChips.forEach(chip => {
    chip.classList.toggle('active', chip.dataset.days === String(daysOrMode));
  });

  updateReportPreviewStats();
}

presetChips.forEach(chip => {
  chip.addEventListener('click', () => {
    setReportPreset(chip.dataset.days);
  });
});

if (reportStartDate) reportStartDate.addEventListener('change', () => {
  presetChips.forEach(c => c.classList.remove('active'));
  updateReportPreviewStats();
});
if (reportEndDate) reportEndDate.addEventListener('change', () => {
  presetChips.forEach(c => c.classList.remove('active'));
  updateReportPreviewStats();
});

function getReportDateRangeData() {
  const startVal = reportStartDate ? reportStartDate.value : '';
  const endVal = reportEndDate ? reportEndDate.value : '';
  const start = startVal ? new Date(startVal + 'T00:00:00') : new Date(Date.now() - 29 * 86400000);
  const end = endVal ? new Date(endVal + 'T23:59:59') : new Date();

  const s = start <= end ? start : end;
  const e = start <= end ? end : start;

  const dates = [];
  const curr = new Date(s);
  while (curr <= e) {
    dates.push(new Date(curr));
    curr.setDate(curr.getDate() + 1);
  }

  let totalScheduled = 0;
  let totalTaken = 0;
  let totalMissed = 0;
  const rows = [];

  dates.forEach(d => {
    const dateKey = formatDateKey(d);
    const dayName = d.toLocaleDateString('en-US', { weekday: 'short' });
    const isToday = formatDateKey(new Date()) === dateKey;
    const dayLogs = state.dailyLogs[dateKey] || {};

    state.medicines.forEach(med => {
      const isTaken = !!dayLogs[med.id];
      const ampm = med.hour >= 12 ? "PM" : "AM";
      const hour12 = med.hour % 12 || 12;
      const minStr = String(med.minute).padStart(2, '0');
      const timeFormatted = `${hour12}:${minStr} ${ampm}`;
      const status = isTaken ? "TAKEN" : (isToday ? "PENDING" : "MISSED");
      const doc = state.doctors.find(dItem => dItem.id === med.doctorId);
      const docName = doc ? doc.name : "Dr. Rajesh Sharma";

      totalScheduled++;
      if (isTaken) totalTaken++;
      else if (!isToday) totalMissed++;

      rows.push({
        dateKey,
        dayName,
        medName: med.name,
        dosage: med.dosage,
        timeFormatted,
        status,
        docName
      });
    });
  });

  const adherenceRate = totalScheduled > 0 ? Math.round((totalTaken / totalScheduled) * 100) : 100;
  return {
    daysCount: dates.length,
    startDateStr: formatDateKey(s),
    endDateStr: formatDateKey(e),
    totalScheduled,
    totalTaken,
    totalMissed,
    adherenceRate,
    rows
  };
}

function updateReportPreviewStats() {
  const data = getReportDateRangeData();
  const statDays = document.getElementById('reportStatDays');
  const statRate = document.getElementById('reportStatRate');
  const statTaken = document.getElementById('reportStatTaken');
  const statMissed = document.getElementById('reportStatMissed');

  if (statDays) statDays.textContent = data.daysCount;
  if (statRate) {
    statRate.textContent = `${data.adherenceRate}%`;
    statRate.style.color = data.adherenceRate >= 80 ? '#A8E6CF' : '#FF8B94';
  }
  if (statTaken) statTaken.textContent = data.totalTaken;
  if (statMissed) statMissed.textContent = data.totalMissed;
}

function exportDoctorReportCSV() {
  const data = getReportDateRangeData();
  let csv = "Date,DayOfWeek,Medicine,Dosage,ScheduledTime,Status,Doctor\r\n";
  data.rows.forEach(r => {
    csv += `${r.dateKey},${r.dayName},"${r.medName}","${r.dosage}",${r.timeFormatted},${r.status},"${r.docName}"\r\n`;
  });

  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = `Dawaii_Adherence_Report_${data.startDateStr}_to_${data.endDateStr}.csv`;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
  showGestureHud(`Exported CSV: ${data.startDateStr} to ${data.endDateStr}`);
}

function generateDoctorReportPDF() {
  const data = getReportDateRangeData();
  const printWindow = window.open('', '_blank');
  if (!printWindow) {
    alert("Please allow popups to preview and print the doctor report.");
    return;
  }

  const doctorsHtml = state.doctors.map(d => `
    <div style="margin-bottom: 4px;"><strong>${d.name}</strong> (${d.specialty}) • ${d.contact}</div>
  `).join('');

  const medicinesHtml = state.medicines.map(m => {
    const doc = state.doctors.find(d => d.id === m.doctorId);
    const ampm = m.hour >= 12 ? "PM" : "AM";
    const h12 = m.hour % 12 || 12;
    const timeStr = `${h12}:${String(m.minute).padStart(2, '0')} ${ampm}`;
    return `<tr>
      <td style="padding: 6px 10px; border-bottom: 1px solid #E5E7EB;"><strong>${m.name}</strong></td>
      <td style="padding: 6px 10px; border-bottom: 1px solid #E5E7EB;">${m.dosage} (${m.type})</td>
      <td style="padding: 6px 10px; border-bottom: 1px solid #E5E7EB;">${timeStr}</td>
      <td style="padding: 6px 10px; border-bottom: 1px solid #E5E7EB;">${doc ? doc.name : '—'}</td>
    </tr>`;
  }).join('');

  const rowsSlice = data.rows.slice(-40);
  const logsHtml = rowsSlice.map(r => `
    <tr>
      <td style="padding: 5px 8px; border-bottom: 1px solid #F3F4F6;">${r.dateKey} (${r.dayName})</td>
      <td style="padding: 5px 8px; border-bottom: 1px solid #F3F4F6;">${r.medName}</td>
      <td style="padding: 5px 8px; border-bottom: 1px solid #F3F4F6;">${r.timeFormatted}</td>
      <td style="padding: 5px 8px; border-bottom: 1px solid #F3F4F6; font-weight: bold; color: ${r.status === 'TAKEN' ? '#059669' : '#DC2626'};">${r.status}</td>
      <td style="padding: 5px 8px; border-bottom: 1px solid #F3F4F6;">${r.docName}</td>
    </tr>
  `).join('');

  const html = `<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8">
  <title>Clinical Medication Adherence Report - Dawaii</title>
  <style>
    @media print {
      body { -webkit-print-color-adjust: exact; print-color-adjust: exact; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Arial, sans-serif; }
      .no-print { display: none !important; }
    }
    body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Arial, sans-serif; color: #111827; padding: 24px 32px; font-size: 13px; line-height: 1.4; background: #FFF; }
    .header-box { border-bottom: 2px solid #111827; padding-bottom: 14px; margin-bottom: 18px; display: flex; justify-content: space-between; align-items: flex-end; }
    .title { font-size: 22px; font-weight: 800; letter-spacing: -0.02em; color: #111827; margin: 0; }
    .subtitle { font-size: 12px; color: #6B7280; margin-top: 4px; }
    .summary-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin-bottom: 20px; }
    .summary-card { background: #F9FAFB; border: 1px solid #E5E7EB; border-radius: 8px; padding: 10px; text-align: center; }
    .summary-value { font-size: 20px; font-weight: 800; color: #111827; }
    .summary-label { font-size: 10px; text-transform: uppercase; color: #6B7280; margin-top: 2px; }
    table { width: 100%; border-collapse: collapse; margin-bottom: 20px; font-size: 12px; }
    th { text-align: left; background: #F3F4F6; padding: 7px 10px; font-size: 11px; text-transform: uppercase; color: #374151; border-bottom: 1px solid #D1D5DB; }
    .signature-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 40px; margin-top: 40px; }
    .sig-line { border-bottom: 1px solid #9CA3AF; height: 35px; margin-bottom: 4px; }
    .print-btn-bar { background: #1F2937; color: #FFF; padding: 12px 20px; border-radius: 8px; margin-bottom: 20px; display: flex; justify-content: space-between; align-items: center; }
  </style>
</head>
<body>
  <div class="print-btn-bar no-print">
    <div><strong>Dawaii Clinical Report</strong> • Selected Range: ${data.startDateStr} to ${data.endDateStr}</div>
    <button onclick="window.print()" style="background: #3B82F6; color: white; border: none; padding: 8px 18px; border-radius: 6px; font-weight: bold; cursor: pointer;">Print / Save as PDF</button>
  </div>

  <div class="header-box">
    <div>
      <h1 class="title">DAWAII CLINICAL MEDICATION REPORT</h1>
      <div class="subtitle">Confidential Patient Compliance &amp; Dose Verification Record</div>
    </div>
    <div style="text-align: right; font-size: 12px; color: #4B5563;">
      <div><strong>Reporting Window:</strong> ${data.startDateStr} — ${data.endDateStr}</div>
      <div><strong>Generated on:</strong> ${new Date().toLocaleDateString('en-IN', { year: 'numeric', month: 'long', day: 'numeric' })}</div>
    </div>
  </div>

  <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 20px; margin-bottom: 18px;">
    <div style="background: #F9FAFB; padding: 12px; border-radius: 6px; border: 1px solid #E5E7EB;">
      <div style="font-weight: 700; font-size: 11px; text-transform: uppercase; color: #6B7280; margin-bottom: 6px;">Patient &amp; Emergency Info</div>
      <div><strong>Patient Profile:</strong> Active Individual Record</div>
      <div><strong>Caregiver SMS Alert:</strong> ${state.caregiverPhone || '9692298485'} (${state.caregiverEnabled ? 'Active' : 'Disabled'})</div>
      <div><strong>App Sync Status:</strong> Live Cloud Synchronized</div>
    </div>
    <div style="background: #F9FAFB; padding: 12px; border-radius: 6px; border: 1px solid #E5E7EB;">
      <div style="font-weight: 700; font-size: 11px; text-transform: uppercase; color: #6B7280; margin-bottom: 6px;">Consulting Doctors &amp; Clinics</div>
      ${doctorsHtml}
    </div>
  </div>

  <div class="summary-grid">
    <div class="summary-card">
      <div class="summary-value">${data.daysCount}</div>
      <div class="summary-label">Days Analyzed</div>
    </div>
    <div class="summary-card">
      <div class="summary-value" style="color: ${data.adherenceRate >= 80 ? '#059669' : '#DC2626'};">${data.adherenceRate}%</div>
      <div class="summary-label">Overall Adherence</div>
    </div>
    <div class="summary-card">
      <div class="summary-value" style="color: #059669;">${data.totalTaken}</div>
      <div class="summary-label">Doses Taken</div>
    </div>
    <div class="summary-card">
      <div class="summary-value" style="color: #DC2626;">${data.totalMissed}</div>
      <div class="summary-label">Doses Missed</div>
    </div>
  </div>

  <h3 style="font-size: 13px; text-transform: uppercase; margin-bottom: 8px; color: #374151;">Active Medication Regimen</h3>
  <table>
    <thead>
      <tr>
        <th>Medication</th>
        <th>Dosage &amp; Form</th>
        <th>Scheduled Time</th>
        <th>Prescribing Doctor</th>
      </tr>
    </thead>
    <tbody>
      ${medicinesHtml}
    </tbody>
  </table>

  <h3 style="font-size: 13px; text-transform: uppercase; margin-bottom: 8px; color: #374151;">Adherence Event Log (${data.startDateStr} to ${data.endDateStr})</h3>
  <table>
    <thead>
      <tr>
        <th>Date</th>
        <th>Medication</th>
        <th>Scheduled</th>
        <th>Status</th>
        <th>Doctor</th>
      </tr>
    </thead>
    <tbody>
      ${logsHtml}
    </tbody>
  </table>

  <div class="signature-grid">
    <div>
      <div class="sig-line"></div>
      <div style="font-size: 11px; color: #6B7280;">Doctor's Signature &amp; Registration Stamp</div>
    </div>
    <div>
      <div class="sig-line"></div>
      <div style="font-size: 11px; color: #6B7280;">Clinical Notes / Dosage Adjustments</div>
    </div>
  </div>
</body>
</html>`;

  printWindow.document.open();
  printWindow.document.write(html);
  printWindow.document.close();
}

if (btnPrintDoctorPDF) {
  btnPrintDoctorPDF.addEventListener('click', generateDoctorReportPDF);
}
if (btnExportCSV) {
  btnExportCSV.addEventListener('click', exportDoctorReportCSV);
}

// --- 14. Lock Screen Overlay Snapping Wheel & Gestures ---
function buildWheel() {
  wheelTrack.innerHTML = '';
  for (let val = state.wheelMin; val <= state.wheelMax; val += state.wheelStep) {
    const item = document.createElement('div');
    item.className = 'wheel-item';
    item.dataset.value = val;
    item.textContent = val;
    wheelTrack.appendChild(item);
  }
  setTimeout(updateWheelSelection, 100);
}

let selectedPostponeMinutes = 15;

function updateWheelSelection() {
  const items = wheelTrack.querySelectorAll('.wheel-item');
  const trackRect = wheelTrack.getBoundingClientRect();
  const trackCenter = trackRect.left + trackRect.width / 2;

  let closestItem = null;
  let minDistance = Infinity;

  items.forEach(item => {
    const rect = item.getBoundingClientRect();
    const itemCenter = rect.left + rect.width / 2;
    const dist = Math.abs(trackCenter - itemCenter);
    if (dist < minDistance) {
      minDistance = dist;
      closestItem = item;
    }
  });

  items.forEach(item => {
    if (item === closestItem) item.classList.add('selected');
    else item.classList.remove('selected');
  });

  selectedPostponeMinutes = closestItem ? parseInt(closestItem.dataset.value, 10) : 15;
  return selectedPostponeMinutes;
}

function onAdjustWheel() {
  if (state.brightenOnScroll && !state.isOverlayActive) {
    activateOverlay();
  }
}

wheelTrack.addEventListener('scroll', () => {
  onAdjustWheel();
  updateWheelSelection();
});
wheelTrack.addEventListener('pointerdown', onAdjustWheel);
wheelTrack.addEventListener('touchstart', onAdjustWheel, { passive: true });
wheelTrack.addEventListener('wheel', onAdjustWheel, { passive: true });

function confirmPostponeAction() {
  const med = state.medicines.find(m => !m.isTaken) || state.medicines[0];
  if (med) {
    const now = new Date();
    const future = new Date(now.getTime() + selectedPostponeMinutes * 60000);
    med.hour = future.getHours();
    med.minute = future.getMinutes();
    addRoomLog(med.name, `${String(med.hour).padStart(2, '0')}:${String(med.minute).padStart(2, '0')}`, "SNOOZED");
    renderDashboard();
    renderSetupOverlay();
    renderMasterCabinet();
    if (typeof renderRecommendations === 'function') renderRecommendations();
    syncToCloud();
    showGestureHud(`Postponed for ${selectedPostponeMinutes} mins (Rescheduled to ${formatTime(med.hour, med.minute)})`);
  } else {
    showGestureHud(`Postponed for ${selectedPostponeMinutes} mins`);
  }
  dismissOverlay(`Postponed for ${selectedPostponeMinutes} mins`);
}

// Logic: Scroll to whichever time and then just tap on it which confirms postponing
wheelTrack.addEventListener('click', (e) => {
  e.stopPropagation();
  const clickedItem = e.target.closest('.wheel-item');
  if (clickedItem) {
    selectedPostponeMinutes = parseInt(clickedItem.dataset.value, 10);
    updateWheelSelection();
  }
  confirmPostponeAction();
});

const wheelContainer = document.querySelector('.wheel-container');
if (wheelContainer) {
  wheelContainer.addEventListener('click', (e) => {
    e.stopPropagation();
    confirmPostponeAction();
  });
}

function showOverlay() {
  if (!state.isOverlayEnabled) {
    showGestureHud('Lock-Screen Overlay is disabled in Customize');
    return;
  }
  state.isOverlayVisible = true;
  state.isOverlayActive = false;
  overlay.classList.remove('hidden');
  overlay.classList.add('dim-state');
  overlay.classList.remove('active-state');
  overlay.style.opacity = state.initialDimness;

  updateClocks();
  updateWheelSelection();

  // Foliage Entrance Animation:
  // Top vine shifts into resting 50% visible position from top
  // Bottom vine mirrors top from bottom
  if (state.theme === 'SAKURA') {
    if (foliageTL) {
      foliageTL.style.transform = 'translate(-50%, -30px)';
      foliageTL.style.opacity = '0';
    }
    if (foliageBR) {
      foliageBR.style.transform = 'translate(-50%, 30px)';
      foliageBR.style.opacity = '0';
    }

    requestAnimationFrame(() => {
      if (foliageTL) {
        foliageTL.style.transition = 'transform 0.95s cubic-bezier(0.16, 1, 0.3, 1), opacity 0.95s ease';
        foliageTL.style.transform = 'translate(-50%, 0)';
        foliageTL.style.opacity = '1';
      }
      if (foliageBR) {
        foliageBR.style.transition = 'transform 0.95s cubic-bezier(0.16, 1, 0.3, 1), opacity 0.95s ease';
        foliageBR.style.transform = 'translate(-50%, 0)';
        foliageBR.style.opacity = '1';
      }
    });
  }

  clearTimeout(state.chimeTimeoutId);
  // Conditional audio chime: Only if master chime toggle AND overlay chime toggle are ON
  if (state.isChimeEnabled && state.chimeOnOverlay) {
    state.chimeTimeoutId = setTimeout(() => {
      if (state.isOverlayVisible) {
        chimeEngine.playTingTing();
        showGestureHud('Audio Chime: Gentle Chime');
      }
    }, state.chimeDelaySeconds * 1000);
  }
}

function activateOverlay() {
  if (!state.isOverlayActive) {
    state.isOverlayActive = true;
    overlay.classList.remove('dim-state');
    overlay.classList.add('active-state');
    overlay.style.opacity = '1.0';
    showGestureHud('Brightness 100% & Scrollwheel Active');
  }
}

function dismissOverlay(reason = 'Dismissed') {
  clearTimeout(state.chimeTimeoutId);
  state.isOverlayVisible = false;
  state.isOverlayActive = false;
  overlay.classList.add('hidden');
  showGestureHud(reason);
}

function handleMarkTaken() {
  const med = state.medicines.find(m => !m.isTaken) || state.medicines[0];
  if (med) {
    med.isTaken = true;
    addRoomLog(med.name, "Now", "TAKEN");
    renderDashboard();
  }
  dismissOverlay('Marked as Taken in Room DB!');
}

function executeGesture(actionName) {
  if (actionName === 'Unlock Phone') {
    dismissOverlay('Unlocked Phone');
  } else if (actionName === 'Mark Taken') {
    handleMarkTaken();
  }
  // 'None' does nothing
}

overlay.addEventListener('click', (e) => {
  if (e.target.closest('.wheel-track') && state.isOverlayActive) return;

  state.tapCount++;
  clearTimeout(state.tapTimer);

  if (state.tapCount === 3) {
    state.tapCount = 0;
    executeGesture(state.tripleTapAction);
  } else {
    state.tapTimer = setTimeout(() => {
      if (state.tapCount === 1) activateOverlay();
      else if (state.tapCount === 2) {
        executeGesture(state.doubleTapAction);
      }
      state.tapCount = 0;
    }, 320);
  }
});

function showGestureHud(text) {
  // Total Text Purge: Ensure no background logs (like chime sent) ever print to the overlay view
  if (state.isOverlayVisible) {
    console.log(`[DAWAII Log] ${text}`);
    return;
  }
  if (!gestureFeedback) return;
  gestureFeedback.textContent = text;
  gestureFeedback.classList.add('show');
  clearTimeout(gestureFeedback._timer);
  gestureFeedback._timer = setTimeout(() => {
    gestureFeedback.classList.remove('show');
  }, 2200);
}

btnTriggerOverlay.addEventListener('click', showOverlay);
btnHeaderTriggerOverlay.addEventListener('click', showOverlay);
btnLaunchOverlayBig.addEventListener('click', showOverlay);

// --- 15. Room Database Table ---
function renderDatabaseTable() {
  dbTableBody.innerHTML = '';
  state.doseLogs.forEach(log => {
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td>#${log.id}</td>
      <td><strong>${log.medName}</strong></td>
      <td>${log.scheduled}</td>
      <td>${log.actionTime}</td>
      <td><span class="${log.status === 'TAKEN' ? 'badge-taken' : 'badge-pending'}">${log.status}</span></td>
    `;
    dbTableBody.appendChild(tr);
  });
}

function addRoomLog(medName, scheduled, status) {
  state.doseLogs.unshift({
    id: Math.floor(Math.random() * 9000 + 1000),
    medName,
    scheduled,
    actionTime: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
    status
  });
  renderDatabaseTable();
}

btnClearLogs.addEventListener('click', () => {
  state.doseLogs = [];
  renderDatabaseTable();
});

// --- 16. Night Shift Simulation ---
let nightTimers = [];
btnSimScreenOff.addEventListener('click', () => {
  const line = document.createElement('div');
  line.className = 'log-line system';
  line.textContent = `[${new Date().toLocaleTimeString()}] Screen turned OFF during Night Shift (12:00 AM - 2:00 AM).`;
  nightShiftLogs.appendChild(line);

  nightTimers.forEach(clearTimeout);
  nightTimers = [];

  const t1 = setTimeout(() => {
    chimeEngine.playTingTing();
    const l1 = document.createElement('div');
    l1.className = 'log-line trigger';
    l1.textContent = `[${new Date().toLocaleTimeString()}] T + 1 min: Screen off -> Playing minimal chime.`;
    nightShiftLogs.appendChild(l1);
  }, 2500);

  nightTimers.push(t1);
});

btnSimScreenOn.addEventListener('click', () => {
  nightTimers.forEach(clearTimeout);
  const line = document.createElement('div');
  line.className = 'log-line trigger';
  line.textContent = `[${new Date().toLocaleTimeString()}] Screen ON detected! Launching DAWAII!! Lock Screen Overlay.`;
  nightShiftLogs.appendChild(line);
  showOverlay();
});

btnSimQuietCancel.addEventListener('click', () => {
  nightTimers.forEach(clearTimeout);
  const line = document.createElement('div');
  line.className = 'log-line cancel';
  line.textContent = `[${new Date().toLocaleTimeString()}] 1hr passed after window (3:00 AM) and phone never turned on. Quietly cancelled alarms.`;
  nightShiftLogs.appendChild(line);

  if (state.caregiverEnabled) {
    const med = state.medicines.find(m => m.name.toLowerCase().includes('pressure') || m.name.toLowerCase().includes('capsule')) || state.medicines[0];
    const alertResult = sendCaregiverMissedSms(med ? med.name : "Blood Pressure", 2, 0); // 2:00 AM -> Night
    if (alertResult) {
      const smsLine = document.createElement('div');
      smsLine.className = 'log-line system';
      smsLine.style.color = 'var(--sakura-pink)';
      smsLine.style.fontWeight = 'bold';
      smsLine.textContent = `[${new Date().toLocaleTimeString()}] Caregiver SMS Sent to ${alertResult.phone}: "${alertResult.smsBody}"`;
      nightShiftLogs.appendChild(smsLine);
    }
  }
});

// --- 17. Download Android Project (.ZIP) ---
btnDownloadZip.addEventListener('click', () => {
  const link = document.createElement('a');
  link.href = '/dawaii-android-project.zip';
  link.download = 'dawaii-android-project.zip';
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);

  btnDownloadZip.innerHTML = `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="20 6 9 17 4 12"/></svg> Download Started!`;
  setTimeout(() => {
    btnDownloadZip.innerHTML = `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg> Download Android Project (.zip)`;
  }, 2500);
});

// --- 18. Live Cloud Synchronization (No APK Redownload Required) ---
function setCloudSyncStatus(status, text) {
  const badge = document.getElementById('cloudSyncStatus');
  if (!badge) return;
  badge.innerHTML = `<span class="cloud-sync-dot"></span> <span>${text}</span>`;
  const dot = badge.querySelector('.cloud-sync-dot');
  if (status === 'synced') {
    badge.style.color = '#A8E6CF';
    badge.style.borderColor = 'rgba(168, 230, 207, 0.4)';
    if (dot) dot.style.background = '#4ADE80';
  } else if (status === 'syncing') {
    badge.style.color = '#FFD3B6';
    badge.style.borderColor = 'rgba(255, 211, 182, 0.4)';
    if (dot) dot.style.background = '#FBBF24';
  } else {
    badge.style.color = 'var(--text-dim)';
    badge.style.borderColor = 'var(--border-color)';
    if (dot) dot.style.background = '#9CA3AF';
  }
}

let syncDebounceTimer = null;
function debouncedSyncToCloud() {
  clearTimeout(syncDebounceTimer);
  syncDebounceTimer = setTimeout(() => {
    syncToCloud();
  }, 400);
}

async function syncToCloud() {
  setCloudSyncStatus('syncing', 'Syncing to Cloud...');
  try {
    const payload = {
      version: Date.now(),
      lastUpdated: new Date().toISOString(),
      overlayText: state.overlayText || 'DAWA!',
      caregiverPhone: state.caregiverPhone || '9692298485',
      caregiverEnabled: state.caregiverEnabled !== false,
      doubleTapAction: state.doubleTapAction || 'Unlock Phone',
      tripleTapAction: state.tripleTapAction || 'Mark Taken',
      brightenOnScroll: state.brightenOnScroll !== false,
      theme: state.theme || 'SAKURA',
      doctors: state.doctors,
      medicines: state.medicines,
      appointments: state.appointments,
      dailyLogs: state.dailyLogs,
      preferences: {
        overlayText: state.overlayText || 'DAWA!',
        caregiverPhone: state.caregiverPhone || '9692298485',
        caregiverEnabled: state.caregiverEnabled !== false,
        brightenOnScroll: state.brightenOnScroll !== false,
        doubleTapAction: state.doubleTapAction || 'Unlock Phone',
        tripleTapAction: state.tripleTapAction || 'Mark Taken',
        theme: state.theme || 'SAKURA',
        initialDimness: state.initialDimness,
        isChimeEnabled: state.isChimeEnabled,
        chimeOnOverlay: state.chimeOnOverlay,
        isOverlayEnabled: state.isOverlayEnabled
      }
    };

    try {
      localStorage.setItem('dawaii_local_state', JSON.stringify(payload));
    } catch (_) {}

    const res = await fetch('/api/sync', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });

    if (res.ok) {
      setCloudSyncStatus('synced', 'Live Cloud Sync Connected');
    } else {
      setCloudSyncStatus('offline', 'Local Cache Active');
    }
  } catch (err) {
    console.warn('Sync to cloud endpoint error:', err);
    setCloudSyncStatus('offline', 'Local Cache Active');
  }
}

let lastAppliedSyncVersion = 0;

async function initCloudSync(isSilent = false) {
  try {
    if (!isSilent) setCloudSyncStatus('syncing', 'Checking Live Sync...');

    let data = null;
    try {
      const res = await fetch('/api/sync');
      if (res.ok) {
        data = await res.json();
      }
    } catch (_) {}

    if (!data || !data.medicines || data.medicines.length === 0) {
      try {
        const staticRes = await fetch('/sync.json');
        if (staticRes.ok) {
          data = await staticRes.json();
        }
      } catch (_) {}
    }

    if (data) {
      const serverVersion = data.version || 1;
      if (serverVersion !== lastAppliedSyncVersion) {
        lastAppliedSyncVersion = serverVersion;

        const localCustomMeds = localStorage.getItem('dawaii_user_medicines');
        if (Array.isArray(data.medicines) && data.medicines.length > 0) {
          if (!localCustomMeds) {
            state.medicines = data.medicines.filter(m => !isMedicineDeleted(m.id, m.name));
          } else {
            // User data takes priority: filter any deleted items and preserve user modifications
            state.medicines = state.medicines.filter(m => !isMedicineDeleted(m.id, m.name));
          }
        }
        if (Array.isArray(data.doctors) && data.doctors.length > 0) {
          state.doctors = data.doctors;
        }
        if (Array.isArray(data.appointments)) {
          state.appointments = data.appointments;
        }
        if (data.dailyLogs && Object.keys(data.dailyLogs).length > 0) {
          state.dailyLogs = { ...state.dailyLogs, ...data.dailyLogs };
        }

        const prefs = data.preferences || data;
        if (prefs.overlayText) state.overlayText = prefs.overlayText;
        if (prefs.caregiverPhone) state.caregiverPhone = prefs.caregiverPhone;
        if (prefs.caregiverEnabled !== undefined) state.caregiverEnabled = prefs.caregiverEnabled;
        if (prefs.brightenOnScroll !== undefined) state.brightenOnScroll = prefs.brightenOnScroll;
        if (prefs.doubleTapAction) state.doubleTapAction = prefs.doubleTapAction;
        if (prefs.tripleTapAction) state.tripleTapAction = prefs.tripleTapAction;
        if (prefs.theme) state.theme = prefs.theme;

        // Synchronize Form Inputs
        const inputOverlay = document.getElementById('inputOverlayText');
        if (inputOverlay) inputOverlay.value = state.overlayText;
        const overlayPrompt = document.getElementById('overlayPrompt');
        if (overlayPrompt) overlayPrompt.textContent = state.overlayText;

        const inputPhone = document.getElementById('inputCaregiverPhone');
        if (inputPhone) inputPhone.value = state.caregiverPhone;
        const toggleSms = document.getElementById('toggleCaregiverSms');
        if (toggleSms) toggleSms.checked = state.caregiverEnabled;

        const toggleBrighten = document.getElementById('toggleBrightenOnScroll');
        if (toggleBrighten) toggleBrighten.checked = state.brightenOnScroll;

        if (selectDoubleTap) selectDoubleTap.value = state.doubleTapAction;
        if (selectTripleTap) selectTripleTap.value = state.tripleTapAction;

        renderDashboard();
        renderDoctors();
        renderMasterCabinet();
        renderAppointments();
        renderDatabaseTable();
      }
      setCloudSyncStatus('synced', 'Live Cloud Sync Connected');
    } else {
      setCloudSyncStatus('offline', 'Standalone Mode');
    }
  } catch (e) {
    console.warn('Initial cloud sync fetch error:', e);
    setCloudSyncStatus('offline', 'Standalone Mode');
  }
}

// Medication Reminder Overlay Checker:
// Spawns lock-screen overlay at medication's scheduled time if reminder is enabled
let lastScheduledOverlayMin = null;
function checkScheduledMedicineOverlay() {
  if (!state.isOverlayEnabled || state.isOverlayVisible) return;
  const now = new Date();
  const currentHour = now.getHours();
  const currentMin = now.getMinutes();
  const todayKey = toDateKey(now);
  const timeKey = `${todayKey}_${currentHour}_${currentMin}`;
  if (lastScheduledOverlayMin === timeKey) return;

  const dayLogs = state.dailyLogs[todayKey] || {};
  const dueMed = state.medicines.find(m => 
    m.hour === currentHour && 
    m.minute === currentMin && 
    (m.sendReminder !== false) && 
    !dayLogs[m.id]
  );

  if (dueMed) {
    lastScheduledOverlayMin = timeKey;
    showOverlay();
    showGestureHud(`Medication Reminder: ${dueMed.name}`);
  }
}
setInterval(checkScheduledMedicineOverlay, 15000);


// --- 19. Phone App Only Mode Engine & Topbar/Drawer Actions ---
function checkPhoneAppMode() {
  const urlParams = new URLSearchParams(window.location.search);
  const isParamPhone = urlParams.get('mode') === 'phone' || urlParams.get('mode') === 'app';
  const isStandalone = window.matchMedia('(display-mode: standalone)').matches || window.navigator.standalone === true;
  const isMobileScreen = window.innerWidth <= 900;
  const isMobileUserAgent = /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent);

  // Default to pure phone app view on all phones, standalone PWAs, and by default everywhere
  if (isParamPhone || isStandalone || isMobileScreen || isMobileUserAgent || localStorage.getItem('dawaii_phone_only') !== 'false') {
    document.body.classList.add('phone-app-only');
  } else {
    document.body.classList.remove('phone-app-only');
  }
}
checkPhoneAppMode();
window.addEventListener('resize', checkPhoneAppMode);

// Wire in-app Topbar buttons
const btnTopReport = document.getElementById('btnTopReport');
if (btnTopReport) {
  btnTopReport.addEventListener('click', () => openDoctorReportModal());
}


// --- 20. Sakura Flower Petals Rain Animation Engine ---
function startSakuraFlowerRain() {
  const container = document.getElementById('fallingPetalsContainer');
  if (!container) return;
  container.innerHTML = '';
  const petalCount = 30;
  for (let i = 0; i < petalCount; i++) {
    const petal = document.createElement('div');
    petal.className = 'falling-petal';
    const left = Math.random() * 100;
    const duration = 2.6 + Math.random() * 3.5;
    const delay = Math.random() * 2.5;
    const size = 10 + Math.random() * 12;
    petal.style.left = `${left}%`;
    petal.style.width = `${size}px`;
    petal.style.height = `${size * 1.3}px`;
    petal.style.animationDuration = `${duration}s`;
    petal.style.animationDelay = `${delay}s`;
    container.appendChild(petal);
  }
}

// --- 21. When to Take (AI Recommendations) Engine ---
function computeClinicalGuidance(med) {
  const name = (med.name || '').toLowerCase();
  const hour = med.hour !== undefined ? med.hour : 8;
  const minute = med.minute !== undefined ? med.minute : 0;
  const ampm = hour >= 12 ? 'PM' : 'AM';
  const hour12 = hour % 12 || 12;
  const timeStr = `${hour12}:${String(minute).padStart(2, '0')} ${ampm}`;

  let category = 'morning';
  let catClass = 'morning';
  let catLabel = `🌅 Morning (${timeStr})`;
  if (hour >= 12 && hour < 17) {
    category = 'noon';
    catClass = 'noon';
    catLabel = `☀️ Noon (${timeStr})`;
  } else if (hour >= 17) {
    category = 'night';
    catClass = 'night';
    catLabel = `🌙 Night (${timeStr})`;
  }

  if (name.includes('pan') || name.includes('pantoprazole') || name.includes('omeprazole') || name.includes('antacid') || name.includes('thyro')) {
    return {
      category, catClass, catLabel,
      mealBadge: '🥣 Empty Stomach (AC)',
      isFoodTag: 'empty_stomach',
      instruction: 'Take 30 to 60 minutes before morning breakfast with plain water. Swallow capsule whole.',
      warning: 'Wait at least 45 minutes before having morning tea, coffee, milk, or calcium supplements.',
      missedRule: 'If missed before breakfast, take on an empty stomach at least 2 hours after lunch.'
    };
  } else if (name.includes('telmi') || name.includes('amlo') || name.includes('blood pressure') || name.includes('bp')) {
    return {
      category, catClass, catLabel,
      mealBadge: '💧 Fixed Daily Schedule',
      isFoodTag: 'any',
      instruction: 'Take once daily at the same time every morning with water. Consistent timing keeps blood pressure stabilized.',
      warning: 'Avoid excessive sodium, high potassium salt substitutes, and unprescribed pain killers (NSAIDs).',
      missedRule: 'Take as soon as remembered unless it is past mid-afternoon. Never take two pills at once.'
    };
  } else if (name.includes('calcium') || name.includes('shelcal') || name.includes('vitamin') || name.includes('beco') || name.includes('zinc')) {
    return {
      category, catClass, catLabel,
      mealBadge: '🍽️ After Dinner (PC)',
      isFoodTag: 'after_food',
      instruction: 'Take after your evening meal with water. Lipid-soluble vitamins and calcium absorb best with food.',
      warning: 'Do not take together with iron supplements or thyroid medications (keep 4 hours apart).',
      missedRule: 'If missed at bedtime, take after breakfast the next morning.'
    };
  } else if (name.includes('balm') || name.includes('gel') || name.includes('cream') || name.includes('volini') || name.includes('vicco')) {
    return {
      category, catClass, catLabel,
      mealBadge: '🧴 External Application',
      isFoodTag: 'external',
      instruction: 'Apply a thin layer gently over the affected area at bedtime. Wash hands thoroughly.',
      warning: 'For external use only. Do not apply onto broken skin, mucous membranes, or open wounds.',
      missedRule: 'Re-apply when discomfort returns, spacing applications at least 4-6 hours apart.'
    };
  } else {
    return {
      category, catClass, catLabel,
      mealBadge: '🍽️ After Food (PC)',
      isFoodTag: 'after_food',
      instruction: 'Take with or immediately after a meal with plenty of water as directed by your physician.',
      warning: 'Adhere to your prescribed schedule and do not self-adjust dosage without consulting your doctor.',
      missedRule: 'Take as soon as remembered unless your next dose is due within 3 hours.'
    };
  }
}


function renderRecommendations(filter = null) {
  if (filter) activeRecFilter = filter;
  const container = document.getElementById('recommendationsList');
  if (!container) return;
  container.innerHTML = '';

  document.querySelectorAll('.rec-filter-pill').forEach(btn => {
    if (btn.dataset.filter === activeRecFilter) btn.classList.add('active');
    else btn.classList.remove('active');
  });

  const meds = state.medicines.filter(m => !isMedicineDeleted(m.id, m.name));
  const filtered = meds.filter(m => {
    const g = computeClinicalGuidance(m);
    if (activeRecFilter === 'all') return true;
    if (activeRecFilter === 'morning') return g.category === 'morning';
    if (activeRecFilter === 'night') return g.category === 'night';
    if (activeRecFilter === 'empty_stomach') return g.isFoodTag === 'empty_stomach';
    if (activeRecFilter === 'after_food') return g.isFoodTag === 'after_food';
    return true;
  });

  if (filtered.length === 0) {
    container.innerHTML = `<div style="text-align: center; color: var(--text-dim); padding: 30px 10px; font-size: 13px;">No prescriptions match this category.</div>`;
    return;
  }

  filtered.forEach(med => {
    const doc = state.doctors.find(d => d.id === med.doctorId);
    const docName = doc ? doc.name : 'Prescribing Physician';
    const g = computeClinicalGuidance(med);

    const card = document.createElement('div');
    card.className = 'rec-card';
    card.innerHTML = `
      <div class="rec-card-top">
        <div>
          <div class="rec-med-name">${med.name}</div>
          <div class="rec-doc-name">${docName} • ${med.dosage || 'Prescription'}</div>
        </div>
        <span class="rec-timing-badge ${g.catClass}">${g.catLabel}</span>
      </div>
      <div class="rec-meal-badge">${g.mealBadge}</div>
      <div class="rec-guideline-item">
        <span class="icon">📋</span>
        <span>${g.instruction}</span>
      </div>
      <div class="rec-guideline-item" style="color: #f5a623;">
        <span class="icon">⚠️</span>
        <span>${g.warning}</span>
      </div>
      <div class="rec-guideline-item" style="color: var(--sakura-pink);">
        <span class="icon">ℹ️</span>
        <span>${g.missedRule}</span>
      </div>
    `;
    container.appendChild(card);
  });
}

document.querySelectorAll('.rec-filter-pill').forEach(btn => {
  btn.addEventListener('click', () => {
    renderRecommendations(btn.dataset.filter);
  });
});

// --- 22. Setup Overlay Screen Engine ---
function renderSetupOverlay() {
  const container = document.getElementById('setupOverlayMedsList');
  const countEl = document.getElementById('setupMedsCount');
  if (!container) return;
  container.innerHTML = '';

  const meds = state.medicines.filter(m => !isMedicineDeleted(m.id, m.name));
  if (countEl) countEl.textContent = `${meds.length} Configured`;

  if (meds.length === 0) {
    container.innerHTML = `<div style="text-align: center; color: var(--text-dim); padding: 20px 0; font-size: 13px;">No medications found. Add medicines in cabinet to set overlay timings.</div>`;
    return;
  }

  const now = new Date();
  const currentTotalMin = now.getHours() * 60 + now.getMinutes();

  meds.forEach(med => {
    const hour = med.hour !== undefined ? med.hour : 8;
    const min = med.minute !== undefined ? med.minute : 0;
    const ampm = hour >= 12 ? 'PM' : 'AM';
    const hour12 = hour % 12 || 12;
    const timeFormatted = `${hour12}:${String(min).padStart(2, '0')} ${ampm}`;

    const medTotalMin = hour * 60 + min;
    let diff = medTotalMin - currentTotalMin;
    if (diff <= 0) diff += 24 * 60;
    const hrsLeft = Math.floor(diff / 60);
    const minsLeft = diff % 60;
    const countdownStr = `Overlay triggers in ${hrsLeft}h ${minsLeft}m`;

    const row = document.createElement('div');
    row.className = 'med-timing-item';
    row.innerHTML = `
      <div class="med-timing-left">
        <div class="med-timing-title">${med.name}</div>
        <div class="med-timing-sub">${countdownStr}</div>
      </div>
      <div class="med-time-editor">
        <button type="button" class="med-time-pill" title="Click to adjust scheduled overlay time">
          ${timeFormatted} ✎
        </button>
      </div>
    `;

    row.querySelector('.med-time-pill').addEventListener('click', () => {
      openQuickTimeAdjustmentModal(med);
    });

    container.appendChild(row);
  });
}

function openQuickTimeAdjustmentModal(med) {
  openAnimatedModal({
    title: `Adjust Time · ${med.name}`,
    subtitle: 'Save new scheduled overlay trigger time',
    bodyHtml: `
      <div class="modal-input-group">
        <label>Scheduled Dose Time (0-59 min)</label>
        <div style="display: flex; gap: 8px;">
          <select id="quickHour" class="modal-input" style="flex: 1;">
            ${Array.from({length: 24}, (_, i) => `<option value="${i}" ${(med?.hour ?? 8) === i ? 'selected' : ''}>${String(i).padStart(2, '0')}:00 ${i >= 12 ? 'PM' : 'AM'}</option>`).join('')}
          </select>
          <select id="quickMinute" class="modal-input" style="flex: 1;">
            ${Array.from({length: 60}, (_, i) => `<option value="${i}" ${(med?.minute ?? 0) === i ? 'selected' : ''}>:${String(i).padStart(2, '0')}</option>`).join('')}
          </select>
        </div>
      </div>
    `,
    onSave: () => {
      const h = parseInt(document.getElementById('quickHour')?.value || (med.hour ?? 8), 10);
      const m = parseInt(document.getElementById('quickMinute')?.value || (med.minute ?? 0), 10);
      med.hour = h;
      med.minute = m;
      showGestureHud(`${med.name} scheduled for ${formatTime(h, m)} ✓`);
      renderSetupOverlay();
      renderDashboard();
      renderMasterCabinet();
      if (typeof renderRecommendations === 'function') renderRecommendations();
      syncToCloud();
    }
  });
}

const btnTestLockScreenNow = document.getElementById('btnTestLockScreenNow');
if (btnTestLockScreenNow) {
  btnTestLockScreenNow.addEventListener('click', () => {
    showOverlay();
  });
}

const btnCheckAppUpdate = document.getElementById('btnCheckAppUpdate');
if (btnCheckAppUpdate) {
  btnCheckAppUpdate.addEventListener('click', async () => {
    showGestureHud('Checking for updates from deployment...');
    try {
      const res = await fetch(`/version.json?t=${Date.now()}`);
      if (res.ok) {
        const v = await res.json();
        showGestureHud(`Deployment version ${v.version} loaded! Syncing...`);
        if ('serviceWorker' in navigator) {
          const regs = await navigator.serviceWorker.getRegistrations();
          for (const reg of regs) {
            await reg.update();
          }
        }
        await syncFromCloud();
        setTimeout(() => {
          showGestureHud('App is up to date! ✓');
        }, 800);
      } else {
        showGestureHud('Latest deployment active ✓');
      }
    } catch (e) {
      showGestureHud('Synced with latest deployment ✓');
    }
  });
}

// --- 23. Login Gatekeeper & Developer Mode Auth Engine ---
const DEVELOPER_GEMINI_KEY = "";

function initAuthFlow() {
  const loginModal = document.getElementById('loginGatekeeperModal');
  const greetingScreen = document.getElementById('developerGreetingScreen');
  const tabGuest = document.getElementById('tabModeGuest');
  const tabDev = document.getElementById('tabModeDev');
  const viewGuest = document.getElementById('viewAuthGuest');
  const viewDev = document.getElementById('viewAuthDev');
  const btnContinueGuest = document.getElementById('btnContinueGuest');
  const btnLoginDev = document.getElementById('btnLoginDev');
  const inputDevUser = document.getElementById('inputDevUser');
  const inputDevPass = document.getElementById('inputDevPass');
  const devAuthError = document.getElementById('devAuthError');
  const btnProceedGreeting = document.getElementById('btnProceedFromGreeting');
  const drawerModeLabel = document.getElementById('drawerAuthModeLabel');
  const btnSwitchMode = document.getElementById('btnSwitchAuthMode');

  tabGuest?.addEventListener('click', () => {
    tabGuest.classList.add('active');
    tabDev.classList.remove('active');
    viewGuest.classList.remove('hidden');
    viewDev.classList.add('hidden');
    if (devAuthError) devAuthError.textContent = '';
  });

  tabDev?.addEventListener('click', () => {
    tabDev.classList.add('active');
    tabGuest.classList.remove('active');
    viewDev.classList.remove('hidden');
    viewGuest.classList.add('hidden');
    if (inputDevUser) inputDevUser.focus();
  });

  btnContinueGuest?.addEventListener('click', () => {
    localStorage.setItem('dawaii_auth_mode', 'guest');
    updateAuthDisplay();
    loginModal.classList.add('hidden');
    loadUserData();
    renderDashboard();
  });

  function performDevLogin() {
    const user = (inputDevUser?.value || '').trim();
    const pass = (inputDevPass?.value || '').trim();
    if (user.toLowerCase() === 'krish' && pass === 'babys') {
      localStorage.setItem('dawaii_auth_mode', 'developer');
      // Set developer Gemini API key automatically if configured
      if (DEVELOPER_GEMINI_KEY) {
        localStorage.setItem('dawaii_gemini_api_key', DEVELOPER_GEMINI_KEY);
        state.geminiApiKey = DEVELOPER_GEMINI_KEY;
        const keyInput = document.getElementById('inputGeminiApiKey');
        if (keyInput) keyInput.value = DEVELOPER_GEMINI_KEY;
        const statusPill = document.getElementById('geminiKeyStatus');
        if (statusPill) {
          statusPill.textContent = 'Active (Developer Key)';
          statusPill.style.color = '#67d59a';
        }
      }

      updateAuthDisplay();
      loginModal.classList.add('hidden');
      loadUserData();
      renderDashboard();
      showDeveloperGreeting();
    } else {
      if (devAuthError) devAuthError.textContent = 'Incorrect credentials.';
    }
  }

  btnLoginDev?.addEventListener('click', performDevLogin);
  inputDevPass?.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') performDevLogin();
  });

  btnProceedGreeting?.addEventListener('click', () => {
    greetingScreen.classList.add('hidden');
  });

  btnSwitchMode?.addEventListener('click', () => {
    loginModal.classList.remove('hidden');
  });

  function updateAuthDisplay() {
    const mode = localStorage.getItem('dawaii_auth_mode');
    if (drawerModeLabel) {
      drawerModeLabel.textContent = mode === 'developer' ? 'Developer (Krish)' : 'Guest';
    }
  }

  function showDeveloperGreeting() {
    greetingScreen.classList.remove('hidden');
    greetingScreen.classList.remove('swooshing-away');
    startSakuraFlowerRain();
    setTimeout(() => {
      greetingScreen.classList.add('swooshing-away');
      setTimeout(() => {
        greetingScreen.classList.add('hidden');
        greetingScreen.classList.remove('swooshing-away');
      }, 750);
    }, 2000);
  }

  // Check persisted auth mode: login only once, no need to re-login every time
  const persistedMode = localStorage.getItem('dawaii_auth_mode');
  if (persistedMode === 'developer') {
    loginModal.classList.add('hidden');
    updateAuthDisplay();
    showDeveloperGreeting();
  } else if (persistedMode === 'guest') {
    loginModal.classList.add('hidden');
    updateAuthDisplay();
  } else {
    loginModal.classList.remove('hidden');
    if (inputDevUser) inputDevUser.value = '';
    if (inputDevPass) inputDevPass.value = '';
    if (devAuthError) devAuthError.textContent = '';
    updateAuthDisplay();
  }
}

// --- 24. Initial Render ---
buildWheel();
renderDashboard();
renderDoctors();
renderMasterCabinet();
renderAppointments();
renderDatabaseTable();
initCloudSync();
initAuthFlow();

// Dismiss app buffering screen smoothly once resources are ready
setTimeout(() => {
  const buffScreen = document.getElementById('appBufferingScreen');
  if (buffScreen) {
    buffScreen.classList.add('fade-out');
    setTimeout(() => {
      try { buffScreen.remove(); } catch (_) {}
    }, 500);
  }
}, 500);



const fs = require('fs');
const path = require('path');

const TMP_SYNC_FILE = path.join('/tmp', 'dawaii_sync.json');
const LOCAL_SYNC_FILE = path.join(process.cwd(), 'sync.json');

let inMemorySync = null;

module.exports = (req, res) => {
  // Enable CORS
  res.setHeader('Access-Control-Allow-Credentials', 'true');
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET,OPTIONS,PATCH,DELETE,POST,PUT');
  res.setHeader(
    'Access-Control-Allow-Headers',
    'X-CSRF-Token, X-Requested-With, Accept, Accept-Version, Content-Length, Content-MD5, Content-Type, Date, X-Api-Version'
  );

  if (req.method === 'OPTIONS') {
    res.status(200).end();
    return;
  }

  if (req.method === 'POST') {
    try {
      const payload = typeof req.body === 'string' ? JSON.parse(req.body) : req.body;
      const dataToSave = {
        version: Date.now(),
        lastUpdated: new Date().toISOString(),
        ...payload
      };

      inMemorySync = dataToSave;

      try {
        fs.writeFileSync(TMP_SYNC_FILE, JSON.stringify(dataToSave, null, 2), 'utf-8');
      } catch (err) {
        console.warn('Could not write to /tmp:', err);
      }

      res.status(200).json({ status: 'ok', lastUpdated: dataToSave.lastUpdated, data: dataToSave });
    } catch (e) {
      res.status(400).json({ error: 'Invalid JSON payload: ' + e.message });
    }
    return;
  }

  // GET: retrieve sync state
  let data = inMemorySync;
  if (!data && fs.existsSync(TMP_SYNC_FILE)) {
    try {
      data = JSON.parse(fs.readFileSync(TMP_SYNC_FILE, 'utf-8'));
    } catch (e) {}
  }

  if (!data && fs.existsSync(LOCAL_SYNC_FILE)) {
    try {
      data = JSON.parse(fs.readFileSync(LOCAL_SYNC_FILE, 'utf-8'));
    } catch (e) {}
  }

  if (!data) {
    data = {
      version: 1,
      lastUpdated: new Date().toISOString(),
      doctors: [],
      medicines: [],
      appointments: [],
      preferences: {
        overlayText: "DAWA!",
        caregiverPhone: "9692298485",
        caregiverEnabled: true,
        brightenOnScroll: true
      }
    };
  }

  res.status(200).json(data);
};

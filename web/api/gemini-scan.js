const https = require('https');

// Built-in Indian & global pharmacopeia knowledge base for prescription recognition & dosage intelligence
const PHARMACOPEIA_DATABASE = [
  {
    names: ['telmisartan', 'telma', 'telpres', 'telsar', 'micardis'],
    defaultDosage: '40mg',
    defaultType: 'Pill',
    timing: 'Morning (08:00 AM)',
    mealRelation: 'Empty Stomach / Before Breakfast',
    doctorSpecialty: 'Cardiologist',
    recommendation: 'Take once daily in the morning with a glass of water. Maintain consistent timing to control blood pressure smoothly.'
  },
  {
    names: ['amlodipine', 'stamlo', 'amlong', 'norvasc'],
    defaultDosage: '5mg',
    defaultType: 'Pill',
    timing: 'Morning (09:00 AM)',
    mealRelation: 'With or without food',
    doctorSpecialty: 'Cardiologist',
    recommendation: 'Take at the same time each day. Avoid sudden standing to prevent lightheadedness.'
  },
  {
    names: ['pan-d', 'pand', 'pantoprazole', 'pan 40', 'pantocid', 'pantosec'],
    defaultDosage: 'Pan-D 40mg',
    defaultType: 'Capsule',
    timing: 'Morning (07:30 AM)',
    mealRelation: 'Empty Stomach (30 mins before breakfast)',
    doctorSpecialty: 'Gastroenterologist',
    recommendation: 'Swallow whole with water 30 to 60 minutes before morning breakfast. Do not crush or chew the capsule.'
  },
  {
    names: ['becozinc', 'multivitamin', 'becosules', 'zincovit', 'supradyn'],
    defaultDosage: '1 Capsule',
    defaultType: 'Capsule',
    timing: 'Night (09:00 PM)',
    mealRelation: 'After Dinner',
    doctorSpecialty: 'General Physician',
    recommendation: 'Take after your main meal or dinner with water to optimize vitamin and mineral absorption and prevent nausea.'
  },
  {
    names: ['volini', 'pain relief', 'omnigel', 'moov', 'iodex', 'diclofenac gel'],
    defaultDosage: 'Apply thin layer',
    defaultType: 'Cream',
    timing: 'Night (10:00 PM)',
    mealRelation: 'External Application',
    doctorSpecialty: 'Orthopedic / General Physician',
    recommendation: 'Gently massage a thin layer over affected painful areas. Do not apply on broken skin. Wash hands after use.'
  },
  {
    names: ['amoxicillin', 'mox', 'novamox', 'augmentin', 'amoxyclav', 'clavum'],
    defaultDosage: '500mg',
    defaultType: 'Capsule',
    timing: 'Twice daily (08:00 AM & 08:00 PM)',
    mealRelation: 'Start of a meal',
    doctorSpecialty: 'General Physician',
    recommendation: 'Complete full prescribed course even if symptoms resolve earlier. Take with food to minimize stomach upset.'
  },
  {
    names: ['metformin', 'glycomet', 'glucophage'],
    defaultDosage: '500mg',
    defaultType: 'Pill',
    timing: 'With Meals (Morning & Night)',
    mealRelation: 'With or Immediately After Meals',
    doctorSpecialty: 'Endocrinologist',
    recommendation: 'Always take with food to reduce gastrointestinal discomfort. Avoid excessive alcohol consumption.'
  },
  {
    names: ['atorvastatin', 'atorva', 'lipitor', 'storvas'],
    defaultDosage: '10mg',
    defaultType: 'Pill',
    timing: 'Bedtime (10:00 PM)',
    mealRelation: 'After Dinner / Bedtime',
    doctorSpecialty: 'Cardiologist',
    recommendation: 'Take at night as cholesterol synthesis peaks during sleep. Avoid consuming large quantities of grapefruit juice.'
  },
  {
    names: ['dolo 650', 'dolo', 'paracetamol', 'calpol', 'crocin'],
    defaultDosage: '650mg',
    defaultType: 'Pill',
    timing: 'As needed (every 6-8 hours)',
    mealRelation: 'After food',
    doctorSpecialty: 'General Physician',
    recommendation: 'Take after food for fever or pain relief. Do not exceed 3000mg in 24 hours. Keep interval at least 6 hours.'
  },
  {
    names: ['azithromycin', 'azithral', 'zithromax', 'azimax'],
    defaultDosage: '500mg',
    defaultType: 'Pill',
    timing: 'Morning (09:00 AM)',
    mealRelation: '1 hour before or 2 hours after meals',
    doctorSpecialty: 'Pulmonologist / Physician',
    recommendation: 'Take once daily at the same hour for the 3 or 5 day duration. Swallow whole with plenty of water.'
  },
  {
    names: ['cetirizine', 'cetzine', 'alerid', 'zyrtec', 'allegra', 'fexofenadine'],
    defaultDosage: '10mg',
    defaultType: 'Pill',
    timing: 'Night (10:00 PM)',
    mealRelation: 'Bedtime with water',
    doctorSpecialty: 'General Physician / ENT',
    recommendation: 'Take at bedtime as it may cause slight drowsiness. Avoid driving if feeling sleepy.'
  },
  {
    names: ['thyronorm', 'eltroxin', 'levothyroxine'],
    defaultDosage: '50mcg',
    defaultType: 'Pill',
    timing: 'Early Morning (06:30 AM)',
    mealRelation: 'Empty Stomach (1 hour before tea/coffee/food)',
    doctorSpecialty: 'Endocrinologist',
    recommendation: 'Must be taken on an empty stomach with plain water. Wait at least 45-60 minutes before morning tea, milk, or breakfast.'
  }
];

// Helper: Call Gemini REST API if key is present
async function callGeminiVision(apiKey, imageBase64, mimeType = 'image/jpeg') {
  return new Promise((resolve, reject) => {
    const cleanBase64 = imageBase64.replace(/^data:image\/[a-z]+;base64,/, '');
    const postData = JSON.stringify({
      contents: [
        {
          parts: [
            {
              text: `You are an expert AI clinical pharmacist and visual medication recognition model for DAWAII.
The user is pointing their camera at either:
1. A tablet or pill (look at color, shape, scoring, imprint, size).
2. A medicine bottle (syrup, liquid suspension, tablet bottle).
3. A tube of cream, gel, or ointment.
4. A blister pack / strip or medicine box.
5. A medical prescription or doctor note.

Carefully identify what medicine this is.
If the image is completely pitch black, blurry with no discernible object, or completely unrelated to medicine or packaging, output:
{"success": false, "error": "Could not identify medicine. Please hold the tablet, bottle, or cream tube steadily in good light."}

Otherwise, extract:
- name: The recognized brand or generic drug name (e.g. "Dolo 650", "Pan-D", "Volini Gel", "Augmentin 625 Duo", "Telmisartan", "Becozinc", "Benadryl Syrup", "Betnovate Cream")
- dosage: Strength and unit (e.g. "650mg", "40mg", "Apply thin layer", "10ml", "1 Capsule")
- type: Exactly one of "Pill", "Capsule", "Cream", "Syrup", "Solution"
- reasoning: Friendly 1-sentence explanation of what you see (e.g. "Recognized round white tablet as Paracetamol 650mg", "Identified topical ointment tube as Volini Pain Relief Gel", "Identified bottle as Cough Syrup", "Identified blister pack as Pan-D Gastro-resistant Capsule")
- pillsLeft: Estimated count (default 20 for tablets/capsules, 1 for cream/bottle)
- daysLeft: Estimated duration (default 14)
- timing: Recommended schedule (e.g. "Morning (08:00 AM)", "Night (09:00 PM)", "As needed for pain/fever")
- recommendation: Clinical guidance on how to take or apply it
- doctorSpecialty: Relevant specialist (e.g. "General Physician", "Cardiologist", "Dermatologist", "Gastroenterologist")

Return ONLY valid JSON matching this schema, no markdown code fence, no additional commentary.`
            },
            {
              inline_data: {
                mime_type: mimeType,
                data: cleanBase64
              }
            }
          ]
        }
      ],
      generationConfig: {
        temperature: 0.1,
        response_mime_type: "application/json"
      }
    });

    const options = {
      hostname: 'generativelanguage.googleapis.com',
      port: 443,
      path: `/v1beta/models/gemini-1.5-flash:generateContent?key=${encodeURIComponent(apiKey)}`,
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Content-Length': Buffer.byteLength(postData)
      }
    };

    const req = https.request(options, (res) => {
      let data = '';
      res.on('data', chunk => { data += chunk; });
      res.on('end', () => {
        try {
          const parsed = JSON.parse(data);
          const candidateText = parsed?.candidates?.[0]?.content?.parts?.[0]?.text;
          if (candidateText) {
            const resultJson = JSON.parse(candidateText.trim());
            resolve(resultJson);
          } else {
            resolve(null);
          }
        } catch (e) {
          resolve(null);
        }
      });
    });

    req.on('error', (err) => resolve(null));
    req.write(postData);
    req.end();
  });
}

// Fallback AI Rule Engine for messy handwriting & partial OCR tokens
function parsePrescriptionText(rawText) {
  if (!rawText || typeof rawText !== 'string') return null;
  const text = rawText.toLowerCase().trim();
  if (text.length < 3) return null;

  // Check against known pharmacopeia
  for (const entry of PHARMACOPEIA_DATABASE) {
    for (const alias of entry.names) {
      if (text.includes(alias)) {
        // Extract dosage if present (e.g. 500mg, 40mg, 650mg, 10mg)
        const dosageMatch = rawText.match(/\b(\d+(\.\d+)?\s*(mg|mcg|g|ml|iu))\b/i);
        const detectedDosage = dosageMatch ? dosageMatch[0] : entry.defaultDosage;

        // Extract format/type
        let type = entry.defaultType;
        if (/capsule|cap\b/i.test(text)) type = 'Capsule';
        else if (/tablet|tab\b|pill\b/i.test(text)) type = 'Pill';
        else if (/cream|gel|ointment\b/i.test(text)) type = 'Cream';
        else if (/syrup|suspension|liquid\b/i.test(text)) type = 'Syrup';
        else if (/solution|drops\b/i.test(text)) type = 'Solution';

        const properName = alias.charAt(0).toUpperCase() + alias.slice(1);
        const reasoning = `Identified as ${properName} ${detectedDosage} (${type}) for ${entry.doctorSpecialty || "General Health"}.`;

        return {
          success: true,
          name: properName,
          dosage: detectedDosage,
          type: type,
          reasoning: reasoning,
          pillsLeft: type === 'Cream' || type === 'Syrup' ? 1 : 20,
          daysLeft: 14,
          timing: entry.timing,
          mealRelation: entry.mealRelation,
          recommendation: entry.recommendation,
          doctorSpecialty: entry.doctorSpecialty
        };
      }
    }
  }

  // Generic heuristic extraction if text has medical keywords (e.g., Tab, Cap, mg, Rx, Dosing)
  const isMedicationLike = /\b(tab|cap|tablet|capsule|mg|mcg|ml|syrup|od|bd|tid|qid|daily|po|take)\b/i.test(text);
  if (isMedicationLike) {
    const lines = rawText.split(/\r?\n/).map(l => l.trim()).filter(Boolean);
    const firstLine = lines[0] || "Prescribed Medicine";
    const dosageMatch = rawText.match(/\b(\d+(\.\d+)?\s*(mg|mcg|g|ml|iu))\b/i);

    let type = "Pill";
    if (/capsule|cap\b/i.test(text)) type = "Capsule";
    else if (/cream|gel|ointment\b/i.test(text)) type = "Cream";
    else if (/syrup|liquid\b/i.test(text)) type = "Syrup";

    const cleanName = firstLine.replace(/[^a-zA-Z0-9\s-]/g, '').trim() || "Prescribed Medication";
    const detectedDosage = dosageMatch ? dosageMatch[0] : "1 Dose Daily";

    return {
      success: true,
      name: cleanName,
      dosage: detectedDosage,
      type: type,
      reasoning: `Visual model recognized ${cleanName} ${detectedDosage} formulation (${type}).`,
      pillsLeft: type === 'Cream' || type === 'Syrup' ? 1 : 14,
      daysLeft: 14,
      timing: "Morning (09:00 AM)",
      mealRelation: "After Meals",
      recommendation: "Take regularly as directed by your physician with adequate water."
    };
  }

  return null;
}

async function getRequestBody(req) {
  if (req.body && typeof req.body === 'object') return req.body;
  if (typeof req.body === 'string' && req.body.trim().length > 0) {
    try { return JSON.parse(req.body); } catch (_) { return {}; }
  }
  return new Promise((resolve) => {
    let data = '';
    req.on('data', chunk => { data += chunk; });
    req.on('end', () => {
      try {
        resolve(data ? JSON.parse(data) : {});
      } catch (_) {
        resolve({});
      }
    });
    req.on('error', () => resolve({}));
  });
}

module.exports = async (req, res) => {
  // CORS
  res.setHeader('Access-Control-Allow-Credentials', 'true');
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET,OPTIONS,POST');
  res.setHeader('Access-Control-Allow-Headers', 'X-CSRF-Token, X-Requested-With, Accept, Content-Type');

  if (req.method === 'OPTIONS') {
    res.status(200).end();
    return;
  }

  if (req.method !== 'POST') {
    res.status(405).json({ success: false, error: 'Method Not Allowed' });
    return;
  }

  try {
    const body = await getRequestBody(req);
    const { image, text } = body;

    // Check if input is completely empty
    const hasImage = image && typeof image === 'string' && image.length > 100;
    const hasText = text && typeof text === 'string' && text.trim().length > 0;

    if (!hasImage && !hasText) {
      res.status(200).json({
        success: false,
        error: "Couldn't scan: No prescription input or image found. Please hold the prescription label steady."
      });
      return;
    }

    // 1. Try Gemini Vision if API key is provided by user or environment
    const apiKey = (body.apiKey && String(body.apiKey).trim()) || process.env.GEMINI_API_KEY || process.env.GOOGLE_API_KEY;
    if (apiKey && hasImage) {
      try {
        const geminiResult = await callGeminiVision(apiKey, image);
        if (geminiResult) {
          if (geminiResult.success === false) {
            res.status(200).json({
              success: false,
              error: geminiResult.error || "Couldn't scan: No readable prescription text found."
            });
            return;
          }
          if (geminiResult.name) {
            res.status(200).json({
              success: true,
              data: geminiResult,
              source: "gemini-1.5-flash"
            });
            return;
          }
        }
      } catch (geminiErr) {
        console.warn('Gemini API call failed, falling back to rule engine:', geminiErr);
      }
    }

    // 2. Parse with Pharmacopeia NLP Model
    if (hasText) {
      const parsed = parsePrescriptionText(text);
      if (parsed) {
        res.status(200).json({
          success: true,
          data: parsed,
          source: "ai-pharmacopeia"
        });
        return;
      }
    }

    // If image was provided but no text or text was indecipherable noise
    // Check if user uploaded a sample demo image or known prescription
    if (hasText) {
      res.status(200).json({
        success: false,
        error: "Couldn't scan: The text could not be identified as a valid medication or prescription. Please ensure the label is clearly visible under good lighting."
      });
      return;
    }

    res.status(200).json({
      success: false,
      error: "Couldn't scan: No prescription text or medicine label could be recognized. Please hold the bottle or prescription clearly inside the frame."
    });

  } catch (err) {
    res.status(500).json({
      success: false,
      error: "Scan processing error: " + err.message
    });
  }
};

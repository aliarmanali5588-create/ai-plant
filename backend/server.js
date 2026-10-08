const http = require('http');

const PORT = process.env.PORT || 3000;
const GROQ_API_KEY = process.env.GROQ_API_KEY;
const GROQ_MODEL = process.env.GROQ_MODEL || 'llama-3.3-70b-versatile';

const server = http.createServer((req, res) => {
  // CORS
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');

  if (req.method === 'OPTIONS') {
    res.writeHead(204);
    res.end();
    return;
  }

  const url = new URL(req.url, `http://${req.headers.host || 'localhost'}`);

  // Health Check
  if (url.pathname === '/health') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ status: 'ok', service: 'PlantCare AI API' }));
    return;
  }

  // Vision Analysis endpoint (Mocking cloud vision provider for Phase 4)
  if (url.pathname === '/api/analyze' && req.method === 'POST') {
    // In production, this would call a real Vision API (e.g., Google Cloud Vision, AWS Rekognition)
    const responseData = {
      plant: "Tomato",
      disease: "Early Blight",
      confidence: 0.87,
      predictions: [
        { name: "Early Blight", confidence: 0.87 },
        { name: "Bacterial Spot", confidence: 0.09 },
        { name: "Healthy", confidence: 0.04 }
      ],
      provider: "PlantCare AI Secure Proxy"
    };
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify(responseData));
    return;
  }

  // Advisory endpoint (LLM Advisory using Groq)
  if (url.pathname === '/api/advisory' && req.method === 'POST') {
    const chunks = [];
    req.on('data', chunk => chunks.push(chunk));
    req.on('end', async () => {
      try {
        const body = JSON.parse(Buffer.concat(chunks).toString());
        const { plant, disease, confidence, predictions } = body;
        
        const prompt = `You are PlantCare AI, an educational plant-care assistant. 
Result from vision model:
Plant: ${plant}
Detected condition: ${disease}
Confidence: ${confidence}

Explain the condition, symptoms, actions, prevention, expert help, and caution in JSON.`;

        const response = await fetch(`https://api.groq.com/openai/v1/chat/completions`, {
          method: 'POST',
          headers: { 
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${GROQ_API_KEY}`
          },
          body: JSON.stringify({
            model: GROQ_MODEL,
            messages: [{ role: 'user', content: prompt }],
            response_format: { type: "json_object" }
          })
        });

        const data = await response.json();
        const advisoryText = data.choices[0].message.content;
        
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(advisoryText);
      } catch (error) {
        console.error('Groq Advisory failed:', error);
        res.writeHead(500, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: 'Failed to generate advisory' }));
      }
    });
    return;
  }

  res.writeHead(404, { 'Content-Type': 'application/json' });
  res.end(JSON.stringify({ error: 'Not found' }));
});

server.listen(PORT, '0.0.0.0', () => {
  console.log(`PlantCare AI API Server listening on port ${PORT}`);
});

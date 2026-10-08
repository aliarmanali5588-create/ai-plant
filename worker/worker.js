export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    const corsHeaders = {
      'Access-Control-Allow-Origin': '*',
      'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
      'Access-Control-Allow-Headers': 'Content-Type, Authorization',
    };

    if (request.method === 'OPTIONS') {
      return new Response(null, { headers: corsHeaders });
    }

    // Health Check
    if (url.pathname === '/health') {
      return new Response(JSON.stringify({ status: 'ok', service: 'PlantCare AI API' }), {
        headers: { ...corsHeaders, 'Content-Type': 'application/json' }
      });
    }

    // Vision Analysis
    if (url.pathname === '/api/analyze' && request.method === 'POST') {
      const responseData = {
        plant: "Tomato",
        disease: "Early Blight",
        confidence: 0.87,
        predictions: [
          { name: "Early Blight", confidence: 0.87 },
          { name: "Bacterial Spot", confidence: 0.09 },
          { name: "Healthy", confidence: 0.04 }
        ],
        provider: "PlantCare AI Worker"
      };
      return new Response(JSON.stringify(responseData), {
        headers: { ...corsHeaders, 'Content-Type': 'application/json' }
      });
    }

    // Advisory
    if (url.pathname === '/api/advisory' && request.method === 'POST') {
      try {
        const body = await request.json();
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
            'Authorization': `Bearer ${env.GROQ_API_KEY}`
          },
          body: JSON.stringify({
            model: env.GROQ_MODEL || 'llama-3.3-70b-versatile',
            messages: [{ role: 'user', content: prompt }],
            response_format: { type: "json_object" }
          })
        });

        const data = await response.json();
        const advisoryText = data.choices[0].message.content;
        
        return new Response(advisoryText, {
          headers: { ...corsHeaders, 'Content-Type': 'application/json' }
        });
      } catch (error) {
        return new Response(JSON.stringify({ error: 'Failed to generate advisory' }), {
          status: 500,
          headers: { ...corsHeaders, 'Content-Type': 'application/json' }
        });
      }
    }

    return new Response(JSON.stringify({ error: 'Not found' }), {
      status: 404,
      headers: { ...corsHeaders, 'Content-Type': 'application/json' }
    });
  }
};

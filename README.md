# 🌿 PlantCare AI

### On-Device Plant Disease Detection + LLM-Powered Plant Health Advisory

PlantCare AI ek Android application hai jo plant ya leaf ki photo lekar uska health status aur possible disease identify karta hai. Iske baad detected result ko use karke ek Large Language Model (LLM) user ko simple language me plant-care guidance deta hai.

Project ka main goal hai ki plant diagnosis fast ho, basic image analysis device par hi ho, aur detailed guidance LLM ke through generate ho.

---

## 📱 PlantCare AI Kya Hai?

PlantCare AI ek Android-based plant health assistant hai.

User ke paas do options hote hain:

- 📷 Camera se plant ki photo lena
- 🖼️ Gallery se plant ki photo choose karna

Photo milne ke baad app:

1. Image ko process karta hai.
2. On-device TensorFlow Lite model image ka analysis karta hai.
3. Plant/disease prediction aur confidence score generate hota hai.
4. Structured diagnosis backend ko bheja jata hai.
5. Backend Groq ke Large Language Model (LLM) ko request bhejta hai.
6. LLM plant ke liye detailed guidance generate karta hai.
7. Final advisory Android app me display hoti hai.

---

## 🎯 Project ka Main Objective

PlantCare AI ka objective hai:

- Plant ki image ko automatically analyze karna
- Possible plant disease ya health condition identify karna
- Prediction ke saath confidence score dikhana
- LLM ke through easy-to-understand plant-care guidance dena
- Treatment aur prevention ke practical suggestions dena
- User ko simple aur useful advice dena

---

## 🧠 Technology Kaise Use Hui Hai?

Is project me do important technology layers hain.

### 1. On-Device Machine Learning

Plant/disease classification ke liye TensorFlow Lite model use kiya gaya hai.

**Model:** `plant_disease_model.tflite`

```text
Plant Image
    ↓
TensorFlow Lite Model
    ↓
Plant / Disease Prediction
    ↓
Confidence Score
```

Ye model Android device ke andar run hota hai.

Iska benefit ye hai ki initial image classification ke liye har baar cloud vision service par depend nahi karna padta.

### 2. Large Language Model (LLM)

Detailed plant-care guidance generate karne ke liye Large Language Model use kiya gaya hai.

**LLM Provider:** Groq

**Current LLM Model:** `llama-3.3-70b-versatile`

LLM ko raw image directly dene ke bajay application pehle structured plant analysis create karta hai.

Example:

```text
Plant: Tomato
Detected Condition: Bacterial Spot
Confidence: 69%
```

Ye information backend ko bheji jati hai.

Backend LLM se detailed guidance generate karwata hai.

---

## 🏗️ Complete Architecture

PlantCare AI ka complete flow:

```text
┌────────────────────────┐
│      Android App       │
│                        │
│   Camera / Gallery     │
└────────────┬───────────┘
             │
             ▼
┌────────────────────────┐
│ Image Processing       │
│ & Preprocessing        │
└────────────┬───────────┘
             │
             ▼
┌────────────────────────┐
│ TensorFlow Lite        │
│ On-Device Model        │
└────────────┬───────────┘
             │
             ▼
┌────────────────────────┐
│ Plant / Disease        │
│ Prediction             │
│ + Confidence           │
└────────────┬───────────┘
             │
             │ HTTPS
             ▼
┌────────────────────────┐
│ Cloudflare Worker      │
│ Backend API            │
└────────────┬───────────┘
             │
             ▼
┌────────────────────────┐
│ Groq                   │
│ Large Language Model   │
└────────────┬───────────┘
             │
             ▼
┌────────────────────────┐
│ Structured Advisory    │
│ Symptoms               │
│ Treatment              │
│ Prevention             │
│ Caution                │
└────────────┬───────────┘
             │
             ▼
┌────────────────────────┐
│ Android Advisory UI    │
└────────────────────────┘
```

---

## 🧩 Main Features

### 📷 Camera Analysis

User directly camera se plant ki photo capture kar sakta hai.

App captured image ko process karke TensorFlow Lite model ke through prediction generate karta hai.

### 🖼️ Gallery Analysis

User gallery se existing plant image choose kar sakta hai.

Android system photo picker ka use karke image select hoti hai.

### 🔬 On-Device Disease Detection

Plant image ka initial analysis device ke andar TensorFlow Lite model se hota hai.

Result me:

- Plant name
- Detected condition
- Confidence score
- Alternative predictions

dikhaye ja sakte hain.

### 🧠 LLM Advisory

Diagnosis ke baad backend LLM detailed guidance generate karta hai.

LLM advisory me include ho sakta hai:

- What is it?
- Symptoms
- What to do
- Organic treatment
- Prevention
- When to seek expert help
- Caution / safety information

### 📊 Confidence Score

Prediction ke saath model confidence bhi display hota hai.

Example:

```text
Bacterial Spot
69% Confidence
```

Alternative predictions bhi dikhaye ja sakte hain.

Example:

```text
Bacterial Spot     69%
Black Rot          22%
Powdery Mildew      2%
```

### 🔄 Retry Support

Agar advisory request fail hoti hai ya network problem aati hai, user retry kar sakta hai.

### 🌗 Light / Dark Theme

Application light aur dark theme support karta hai.

### 🎨 Claymorphism UI

Application soft, rounded aur organic Claymorphism-inspired visual style use karta hai.

---

## 🎨 UI Design

PlantCare AI ka visual design organic aur soft clay-style interface par based hai.

Important design elements:

- Rounded cards
- Soft shadows
- Clay-style buttons
- Green botanical visual language
- Large readable typography
- Light and dark theme
- Plant-focused icons
- Simple navigation

UI ka main goal hai ki application modern hone ke saath easy-to-understand bhi rahe.

---

## 🧱 Technology Stack

### Android

- Kotlin
- Jetpack Compose
- Material 3
- Android Activity Result APIs
- Camera integration
- Android system photo picker

### Machine Learning

- TensorFlow Lite
- Pre-trained plant disease classification model
- On-device inference
- MobileNet-based architecture

### Backend

- Cloudflare Workers
- JavaScript Worker runtime
- HTTPS REST API

### LLM

- Groq
- `llama-3.3-70b-versatile`

### Networking

- Retrofit
- OkHttp
- Moshi

---

## 🔐 Security

PlantCare AI ka important security rule hai:

> LLM/API credentials Android app ke andar store nahi kiye jate.

Groq API key Android application me hardcode nahi hai.

Architecture:

```text
Android App
     │
     │ HTTPS
     ▼
Cloudflare Worker
     │
     │ Server-side secret
     ▼
Groq API
```

Iska benefit ye hai ki sensitive Groq credential client-side APK me expose nahi hota.

---

## 🌐 Backend API

Production backend Cloudflare Worker par deployed hai.

### Base URL

`https://ai-plant.aliarmanal5588.workers.dev`

### Health Check

```http
GET /health
```

Expected response:

```json
{
  "status": "ok",
  "service": "PlantCare AI API"
}
```

### Advisory API

```http
POST /api/advisory
```

Ye endpoint Android app ke structured plant analysis ko receive karta hai aur LLM-generated advisory return karta hai.

Typical flow:

```text
PlantAnalysisResult
        ↓
Cloudflare Worker
        ↓
Groq LLM
        ↓
PlantAdvisory
```

---

## 📦 Android Application Details

**Application Name:** PlantCare AI 🌿

**Application ID:** `com.aistudio.plantcareai.plcrf`

**Version:** 1.0

**Platform:** Android

**Release APK:** ~38 MB

---

## 📁 Important Project Structure

```text
ai-plant/
│
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── com/
│   │   │   │       └── example/
│   │   │   │           ├── model/
│   │   │   │           ├── services/
│   │   │   │           └── ...
│   │   │   │
│   │   │   ├── assets/
│   │   │   │   └── plant_disease_model.tflite
│   │   │   │
│   │   │   └── AndroidManifest.xml
│   │   │
│   │   └── test/
│   │
│   ├── build.gradle.kts
│   └── ...
│
├── worker/
│   ├── worker.js
│   └── wrangler.toml
│
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

---

## 🤖 LLM Ka Role

Is project me LLM ka role primarily **natural-language advisory generation** hai.

TFLite model plant/disease condition identify karta hai.

Example:

```text
Plant me Bacterial Spot detect hua.
```

Uske baad LLM user ko natural language me guidance deta hai:

```text
Is condition ke common symptoms ye ho sakte hain...

Plant ko improve karne ke liye ye steps try karein...

Future me infection prevent karne ke liye...

Severe condition me expert help lene par consider karein...
```

Isliye PlantCare AI ke architecture ko simple way me describe kiya ja sakta hai:

> **On-Device Machine Learning for detection + Large Language Model for intelligent advisory.**

---

## 🧪 Testing

Project me multiple levels par testing ki gayi hai.

### Build Testing

Android project ka release build successfully generate kiya gaya.

### Unit / Robolectric Testing

Testing me important application logic verify kiya gaya, including:

- TFLite model asset availability
- Prediction parsing
- Confidence threshold
- API request serialization
- API response parsing
- ViewModel state transitions

### Real Device Testing

Application ko real Android device par test kiya gaya.

Tested flows:

```text
Gallery → Prediction → LLM Advisory
```

```text
Camera → Prediction → LLM Advisory
```

---

## ✅ Current Demo Status

```text
✅ App Launch
✅ Camera
✅ Gallery
✅ Image Processing
✅ TFLite Model
✅ Plant/Disease Prediction
✅ Confidence Score
✅ Cloudflare Backend
✅ Groq LLM
✅ LLM Advisory
✅ Retry Handling
✅ Light Theme
✅ Dark Theme
✅ Release APK
✅ Real Device Testing
```

---

## 💻 Source Code

Complete source code GitHub repository me available hai.

👉 **[💻 View Source Code](https://github.com/aliarmanal5588-create/ai-plant)**

---

## 📱 Download PlantCare AI APK

Latest release APK GitHub Releases par available hai.

👉 **[📱 Download Latest APK](https://github.com/aliarmanal5588-create/ai-plant/releases/latest/download/app-release.apk)**

APK download karne ke baad Android device par install kiya ja sakta hai.

Typical usage:

```text
PlantCare AI
      ↓
Scan a Plant
      ↓
Take / Select Photo
      ↓
Plant Diagnosis
      ↓
LLM Advisory
```

---

## 🚀 App Kaise Run Karein?

### Method 1 — Android Studio

Project ko Android Studio me open karein.

Gradle sync complete hone ke baad Android device ya emulator connect karein.

Phir application run karein.

### Method 2 — Release APK

GitHub Releases se latest APK download karein aur Android device par install karein.

---

## 🌱 Demo Flow

Ek typical demo me ye steps follow kiye ja sakte hain:

### Step 1

PlantCare AI open karein.

### Step 2

`Scan a Plant` select karein.

### Step 3

Camera se plant ki image capture karein ya gallery se image select karein.

### Step 4

App image ko locally process karega.

### Step 5

TensorFlow Lite prediction generate karega.

Example:

```text
Tomato
Bacterial Spot
69% Confidence
```

### Step 6

Detected result backend ko bheja jayega.

### Step 7

Groq LLM detailed advisory generate karega.

### Step 8

App user ko guidance display karega.

---

## 🧠 Why On-Device ML + LLM?

PlantCare AI ka architecture intentionally hybrid hai.

### On-Device ML ka benefit

- Fast initial prediction
- Image classification device par hoti hai
- Cloud vision service par dependency kam hoti hai
- Basic diagnosis ke liye local inference available hai

### LLM ka benefit

Traditional classification model mainly ek prediction label provide karta hai.

LLM us result ko human-readable guidance me convert kar sakta hai.

Example:

```text
Prediction:
Bacterial Spot
```

becomes:

```text
Possible symptoms:
Leaves par dark spots appear ho sakte hain.

What to do:
Affected leaves ko remove karein...
```

Is combination se detection aur explanation dono possible hote hain.

---

## 🔄 Complete Data Flow

```text
User Plant Photo
       ↓
Android App
       ↓
TensorFlow Lite
       ↓
Plant / Disease Prediction
       ↓
Confidence Score
       ↓
Cloudflare Worker
       ↓
Groq LLM
       ↓
LLM Generated Guidance
       ↓
Android Advisory Screen
```

---

## 🎓 University Project Explanation

Agar project ko college/university ke saamne short me explain karna ho, toh:

> **PlantCare AI ek Android-based plant health assistant hai. Is application me TensorFlow Lite ka use karke plant image ka on-device classification kiya jata hai. Prediction aur confidence score ko secure Cloudflare Worker backend ke through Groq ke Large Language Model ko bheja jata hai. LLM user ke liye symptoms, treatment, prevention aur general plant-care guidance generate karta hai. Is tarah project on-device machine learning aur LLM technology ko combine karta hai.**

---

## 👨‍💻 Project Architecture in One Line

> **On-Device ML for plant disease detection + Cloudflare backend + Groq LLM for intelligent plant-care advisory.**

---

## ⚠️ Important Disclaimer

PlantCare AI educational aur informational purpose ke liye banaya gaya hai.

Application ke predictions aur recommendations professional agricultural diagnosis ka replacement nahi hain.

Severe plant disease, large-scale crop loss, chemical treatment, ya high-risk agricultural situation me qualified agricultural expert ki advice lena recommended hai.

---

## 🔒 Secrets and Environment Variables

Sensitive credentials repository me commit nahi karne chahiye.

Important backend variables:

```text
GROQ_API_KEY
GROQ_MODEL
```

`GROQ_API_KEY` ko secret ke roop me store kiya jata hai.

`GROQ_MODEL`:

```text
llama-3.3-70b-versatile
```

Android application me Groq API key store nahi ki jati.

---

## ☁️ Cloudflare Worker Configuration

Worker source:

```text
worker/worker.js
```

Wrangler configuration:

```text
worker/wrangler.toml
```

Current Worker name:

```text
ai-plant
```

Worker entry point:

```text
worker.js
```

---

## 🌿 Final Summary

PlantCare AI ka main idea simple hai:

```text
Photo lo
   ↓
Plant ko identify karo
   ↓
Possible disease detect karo
   ↓
Confidence dikhao
   ↓
LLM se samjho problem kya hai
   ↓
Treatment aur prevention guidance pao
```

PlantCare AI ka goal sirf disease ka naam batana nahi hai.

Goal hai:

> **"Plant ki problem ko samajhna aur user ko understandable guidance dena."**

---

## ⭐ Project Status

**Status: Demo Ready ✅**

Core flow:

```text
Camera ✅
Gallery ✅
On-Device TFLite ✅
Plant Detection ✅
Disease Prediction ✅
Cloudflare API ✅
Groq LLM ✅
LLM Advisory ✅
Android Release APK ✅
Real Device Testing ✅
```

---

## 🌿 PlantCare AI

### *See the plant. Understand the problem. Get the guidance.*

Made for learning, experimentation and plant-care assistance.

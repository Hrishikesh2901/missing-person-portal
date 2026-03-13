# Missing Person Identification System

![Issues](https://img.shields.io/github/issues/gaganmanku96/Finding-missing-person-using-AI) ![Stars](https://img.shields.io/github/stars/gaganmanku96/Finding-missing-person-using-AI?style=social)
![CodeRabbit Reviews](https://img.shields.io/coderabbit/prs/github/gaganmanku96/Finding-missing-person-using-AI?utm_source=oss&utm_medium=github&utm_campaign=gaganmanku96%2FFinding-missing-person-using-AI&labelColor=171717&color=FF570A&link=https%3A%2F%2Fcoderabbit.ai&label=CodeRabbit+Reviews)

![Streamlit](https://img.shields.io/badge/Streamlit-000000?style=for-the-badge&logo=streamlit&logoColor=white)
![MediaPipe](https://img.shields.io/badge/MediaPipe-000000?style=for-the-badge&logo=mediapipe&logoColor=white)
![Python](https://img.shields.io/badge/Python-000000?style=for-the-badge&logo=python&logoColor=white)
![SQLite](https://img.shields.io/badge/SQLite-000000?style=for-the-badge&logo=sqlite&logoColor=white)

> [![LinkedIn](https://i.stack.imgur.com/gVE0j.png) Endorse on LinkedIn](https://www.linkedin.com/in/gaganmanku96/) if this project was helpful.

---

> **Disclaimer**
>
> All images of individuals appearing in the screenshots and used as sample data in this project were sourced from the internet solely for the purpose of demonstrating the facial recognition pipeline in a non-commercial, educational context. These images are the property of their respective owners. No claim of ownership is made. If you are the rights holder of any image and wish it to be removed, please open an issue and it will be taken down promptly.
>
> This project does not store, distribute, or commercialise any personal images. The face data derived from sample images (landmark vectors) is used only locally for matching demonstration and is not shared with any third party.

---

## The Problem

Hundreds of people — mostly children — go missing every day in India. When a sighting is reported, officers have to manually compare photos, sift through paperwork, and coordinate across stations. By the time a match is confirmed, the trail has often gone cold.

---

## A Case, Start to Finish

**Step 1 — Family files a report. Officer registers the case.**

A family in Haridwar reports their child missing. An officer opens the portal, uploads a photo, and the AI immediately detects the face and extracts a 468-point mesh — no manual tagging needed.

<img src="./assets/screenshots/register_new_case.png" alt="Register New Case — face detected with bounding box" width="700"/>

---

**Step 2 — The dashboard tracks every open case.**

The officer's home screen shows live counts of found and not-found cases, and a map that plots where cases are concentrated across India.

<img src="./assets/screenshots/homepage.png" alt="Officer dashboard with case counts and India map" width="700"/>

---

**Step 3 — A member of the public submits a sighting.**

Someone recognises the person and submits a photo through the public portal (no login required). The same face mesh is extracted and stored.

When an admin clicks **Match Cases**, the KNN model compares all sightings against all open cases. If a face is close enough, the case is automatically flipped to **Found** and the complainant is notified by email.

<img src="./assets/screenshots/view_cases.png" alt="View cases — Found status with sighting location and submitter details" width="700"/>

---

**Step 4 — The city map tells the bigger picture.**

Admins can see which cities have the most unresolved cases and track resolution rates over time.

<img src="./assets/screenshots/cases_by_city.png" alt="Cases by city — India map with city summary table" width="700"/>

---

## How It Works

1. **Officer registers a case** → uploads a photo → AI extracts a 468-point face mesh
2. **Public submits a sighting** → uploads a photo or video → same extraction
3. **Admin clicks Refresh** → KNN matches faces across both datasets → email sent to complainant on match

No manual photo comparison. No paperwork pile-up.

---

## Features

| Feature | Details |
|---|---|
| Face detection | MediaPipe Face Landmarker — highlights detected faces, handles multiple people in frame |
| AI matching | KNN on 1,404-dimensional face vectors; shows confidence % |
| Video sightings | Upload a video — unique faces extracted automatically per frame |
| Live map | Dashboard map showing case density by city across India |
| Email alerts | Auto-notifies complainant email when a match is confirmed |
| Role-based access | Admins can match, edit, delete; Officers can register and view |
| Public portal | Separate mobile-friendly submission page, no login needed |

---

## Getting Started

```bash
git clone https://github.com/gaganmanku96/Finding-missing-person-using-AI.git
cd Finding-missing-person-using-AI
pip install -r requirements.txt
```

Copy and configure credentials:
```bash
cp login_config.yml.example login_config.yml  # edit with your credentials
```

Run the officer/admin portal:
```bash
streamlit run Home.py
```

Run the public submission portal:
```bash
streamlit run mobile_app.py
```

The SQLite database and face landmarker model (~30 MB, auto-downloaded on first use) are created automatically.

### Optional: Email notifications

Set these environment variables to enable email alerts on match:
```
SMTP_HOST, SMTP_PORT, SMTP_USER, SMTP_PASSWORD
```
The complainant's email entered during case registration is used as the recipient.

---

## Tech Stack

- **Streamlit** — UI for both portals
- **MediaPipe Tasks** — face mesh landmark extraction (468 points × 3D)
- **scikit-learn KNN** — face matching
- **SQLModel + SQLite** — data storage
- **Folium** — interactive map
- **OpenCV** — video frame extraction

---

*Thanks to the [MediaPipe](https://mediapipe.dev/) team for the open-source face landmarker model.*

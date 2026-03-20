# --- STAGE 1: Builder ---
FROM python:3.10-slim AS builder

WORKDIR /app

# Install build dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    gcc \
    && rm -rf /var/lib/apt/lists/*

# Copy only requirements to leverage Docker cache
COPY requirements.txt .

# Install python dependencies to the user site-packages directory
RUN pip install --user --no-cache-dir --default-timeout=1000 -r requirements.txt


# --- STAGE 2: Final Runtime ---
FROM python:3.10-slim

WORKDIR /app

# Install runtime dependencies (libgl1 replaces the old libgl1-mesa-glx)
RUN apt-get update && apt-get install -y --no-install-recommends \
    libgl1 \
    libglib2.0-0 \
    && rm -rf /var/lib/apt/lists/*

# Copy only the installed python packages from the builder stage
COPY --from=builder /root/.local /root/.local
# Copy the rest of your application code
COPY . .

# Update PATH so the system can find streamlit and other binaries
ENV PATH=/root/.local/bin:$PATH

# Streamlit default port
EXPOSE 8501

# Command to run the app
CMD ["streamlit", "run", "Home.py", "--server.port=8501", "--server.address=0.0.0.0"]
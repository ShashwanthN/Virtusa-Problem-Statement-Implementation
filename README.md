# Virtusa Assessment

Two problem statements have be merged to gether. Problem statement 1: Online Quiz and Assessment System, Problem statement 2: Resume Analyzer and Job Matcher. This was done so that they could have a common interface while achiving completion of both the problem statements

This project also has a LLM integration. I personally run Qwen 3.5 9B 4bit quant locally on my system via LMstudio. You can use any OpenAI comparable endpoint via ollama or llama.cpp too. Just replace the url in services.py. Job matching works but suggestions wont work without LLM integration.

## What It Does

- Create jobs with custom quiz questions or auto-generate them using an LLM -> Java Statement + Python
- Upload a resume to find matching job positions based on skills -> Python Statement
- Get detailed analysis of how your resume matches a specific job -> Python Statement
- Take quizzes and get pass/fail results based on correct answers -> Java Statement
- See all available jobs and apply to them -> Java + Python

## Project Structure

**frontend/** 

**job-service/** - A Spring Boot Java backend and it manages jobs, quizzes. it then coonnects to the Python service for Resume Matching and AI features.

**python-ai/** - A Python service that handles LLM quiz generation and resume analysis and resume matching.

## Setup

### Versions

- Java 11+ (for job-service it should be supported I didnt use any special APIs but it was developed via Java 21)
- Python 3.9+ (for python-ai)

### Important!!!!!

- Make sure you run the python service before the java service because I have a function that populates python db with job description to keep it simple

### Python AI Service

1. Navigate to the python-ai folder:
```
cd python-ai
```

2. Create a virtual environment:
```
python -m venv venv
source venv/bin/activate 

or 

Just run the run.sh and it should do all the work
```

3. Install dependencies:
```
pip install -r requirements.txt
```

4. Set your environment variables (create a .env file):
```
OPENAI_API_KEY=your_key_here
```

5. Run the service:
```
python main.py
```

The Python service will start on http://localhost:8000

### Job Service (Backend)

1. Navigate to the job-service folder:
```
cd job-service
```
2. Env Variables:

```
You might need to change the env variables such as for sql DB setting like creds and endpoints. 
```
2. Build the project:
```
./mvnw clean build
```

3. Run the application:
```
./mvnw spring-boot:run
```

The backend will start on http://localhost:8080

### Frontend

1. Navigate to the frontend folder:
```
cd frontend
```

2. Serve the files locally (you can use any simple HTTP server):
```
python -m http.server 3000
```

Or just open `index.html` directly in your browser.

3. Access the application at http://localhost:3000

## How to Use

### Creating a Job

1. Click the Create tab
2. Fill in job details
3. Click Next to move to quiz creation
4. Either manually add questions or click "Generate Questions" to have the LLM create them for you
5. Configure the number of required correct answers and test duration
6. Click "Save Job"

### Matching Resumes

1. Go to the "Match" tab
2. Upload a PDF resume
3. The system will analyze your resume and show jobs you're a good match for
4. Click "Analysis" on any job to see detailed feedback on matched/missing skills, score, and LLM suggestions

### Taking a Quiz

1. Click "Apply" on any job
2. Answer all questions (multiple choice)
3. Click "Submit Test"
4. You'll see if you passed and your score

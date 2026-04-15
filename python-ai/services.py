import io
import json
import nltk
import requests
from PyPDF2 import PdfReader
from nltk.corpus import stopwords
from nltk.stem import PorterStemmer
from nltk.tokenize import RegexpTokenizer

try:
    nltk.data.find("corpora/stopwords")
except LookupError:
    nltk.download("stopwords")


class PythonBackendService:

    def __init__(self):
        self.lm_studio_url = "http://localhost:1234/v1/chat/completions"
        self.stop_words = set(stopwords.words("english"))
        self.stemmer = PorterStemmer()
        self.tokenizer = RegexpTokenizer(r"[A-Za-z0-9+#.]+")

    def index_job(self, payload):
        from database import db_instance
        job_id = payload.get("jobId")
        db_instance.save_job_if_missing(job_id, payload)

    def get_indexed_job_ids(self):
        from database import db_instance
        return db_instance.get_all_job_ids()

    def generate_quiz(self, payload):
        title = payload.get("title")
        desc = payload.get("description")
        skills = payload.get("skillsNeeded")
        experience = payload.get("minExperienceYears")
        num_q = int(payload.get("numQuestions") or 5)

        prompt = "Create exactly " + str(num_q) + " technical multiple choice questions for a " + str(title) + " job. "
        prompt = prompt + "Skills needed: " + str(skills) + ". "
        prompt = prompt + "Description: " + str(desc) + ". "

        if experience is not None:
            prompt = prompt + "The candidate has " + str(experience) + " years of minimum experience. Ensure the questions are appropriately difficult for this level of experience. "

        prompt = prompt + "Return ONLY a JSON array with exactly " + str(num_q) + " questions in this exact format: [{\"question\": \"...\", \"options\": [\"A..\", \"B..\", \"C..\", \"D..\"], \"correctOptionIndex\": 0}]. Do not wrap in markdown or any other text."

        request_body = {
            "messages": [
                {"role": "system", "content": "You are an expert technical interviewer."},
                {"role": "user", "content": prompt}
            ],
            "temperature": 0.3
        }

        try:
            response = requests.post(self.lm_studio_url, json=request_body, timeout=160)
            content = response.json()["choices"][0]["message"]["content"]

            if content.startswith("```json"):
                content = content.replace("```json", "", 1)
                content = content.replace("```", "")

            quiz_data = json.loads(content.strip())
            cleaned_questions = self._normalize_questions(quiz_data, num_q, skills, title)
            return {"questions": cleaned_questions}
        except Exception as e:
            print("Error parsing LLM response: " + str(e))
            raise RuntimeError("Quiz generation failed")

    def match_resume(self, file_content):
        from database import db_instance

        text = self._extract_resume_text(file_content)
        resume_lower = text.lower()
        resume_tokens = set(self._normalize_tokens(text))

        matched_job_ids = []
        matches = []
        all_jobs = db_instance.get_all_jobs()

        for jid in all_jobs:
            job = all_jobs.get(jid)
            match_data = self._evaluate_job_match(job, resume_tokens, resume_lower)

            matched_job_ids.append(jid)
            matches.append({
                "jobId": jid,
                "matchScore": match_data["matchScore"],
                "matchedSkills": match_data["matchedSkills"],
                "missingSkills": match_data["missingSkills"],
                "analysisSummary": match_data["analysisSummary"],
                "recommendation": match_data["recommendation"]
            })

        return {
            "jobIds": matched_job_ids,
            "matches": matches
        }

    def analyze_job_match(self, file_content, job_payload):
        text = self._extract_resume_text(file_content)
        resume_lower = text.lower()
        resume_tokens = set(self._normalize_tokens(text))
        match_data = self._evaluate_job_match(job_payload, resume_tokens, resume_lower)

        llm_suggestion = self._generate_llm_match_suggestion(text, job_payload, match_data)

        response = dict(match_data)
        response["llmSuggestion"] = llm_suggestion
        return response

    def _extract_resume_text(self, file_content):
        pdf = PdfReader(io.BytesIO(file_content))
        text = ""

        for page in pdf.pages:
            extracted = page.extract_text()
            if extracted is not None:
                text = text + extracted + " "

        return text

    def _evaluate_job_match(self, job, resume_tokens, resume_lower):
        raw_skills = str(job.get("skillsNeeded") or "")
        skill_list = []
        for skill in raw_skills.split(","):
            clean_skill = skill.strip()
            if clean_skill:
                skill_list.append(clean_skill)

        matched_skills = []
        missing_skills = []
        for skill in skill_list:
            if self._skill_matches(skill, resume_tokens, resume_lower):
                matched_skills.append(skill)
            else:
                missing_skills.append(skill)

        if len(skill_list) > 0:
            score = (len(matched_skills) / len(skill_list)) * 100
        else:
            score = 100.0

        return {
            "matchScore": round(score),
            "matchedSkills": matched_skills,
            "missingSkills": missing_skills,
            "analysisSummary": self._build_analysis_summary(job.get("title"), matched_skills, missing_skills, len(skill_list)),
            "recommendation": self._build_recommendation(missing_skills)
        }

    def _generate_llm_match_suggestion(self, resume_text, job_payload, match_data):
        prompt = (
            "You are helping a candidate understand how well their resume fits a job. "
            "Read the resume and the job details, then give a short practical suggestion with 3 parts: "
            "1) fit summary, 2) strongest alignment, 3) what to improve next. "
            "Keep it under 120 words.\n\n"
            "Job title: " + str(job_payload.get("title") or "") + "\n"
            "Job description: " + str(job_payload.get("description") or "") + "\n"
            "Skills needed: " + str(job_payload.get("skillsNeeded") or "") + "\n"
            "Match score: " + str(match_data.get("matchScore")) + "%\n"
            "Matched skills: " + ", ".join(match_data.get("matchedSkills") or []) + "\n"
            "Missing skills: " + ", ".join(match_data.get("missingSkills") or []) + "\n\n"
            "Resume text:\n" + str(resume_text[:6000])
        )

        request_body = {
            "messages": [
                {"role": "system", "content": "You are a concise career coach."},
                {"role": "user", "content": prompt}
            ],
            "temperature": 0.4
        }

        try:
            response = requests.post(self.lm_studio_url, json=request_body, timeout=160)
            content = response.json()["choices"][0]["message"]["content"]
            return str(content).strip()
        except Exception as e:
            print("Error generating LLM match suggestion: " + str(e))
            return match_data.get("recommendation") or "Highlight the strongest matching skills and strengthen the missing ones."

    def _normalize_tokens(self, text):
        tokens = self.tokenizer.tokenize((text or "").lower())
        normalized = []

        for token in tokens:
            if token in self.stop_words:
                continue
            if not any(char.isalnum() for char in token):
                continue
            normalized.append(self.stemmer.stem(token))

        return normalized

    def _skill_matches(self, skill, resume_tokens, resume_lower):
        skill_lower = (skill or "").strip().lower()
        if not skill_lower:
            return False

        if skill_lower in resume_lower:
            return True

        skill_tokens = self._normalize_tokens(skill_lower)
        if not skill_tokens:
            return False

        for token in skill_tokens:
            if token not in resume_tokens:
                return False
        return True

    def _build_analysis_summary(self, job_title, matched_skills, missing_skills, total_skills):
        if total_skills == 0:
            return "This role has no listed skills, so the resume is treated as a full match."

        if len(missing_skills) == 0:
            return "The resume covers all listed skills for the " + str(job_title) + " role."

        if len(matched_skills) == 0:
            return "The resume does not clearly show any of the listed skills for the " + str(job_title) + " role yet."

        return "The resume shows " + str(len(matched_skills)) + " of " + str(total_skills) + " listed skills for the " + str(job_title) + " role."

    def _build_recommendation(self, missing_skills):
        if len(missing_skills) == 0:
            return "Ready to apply based on the listed job skills."

        if len(missing_skills) == 1:
            return "Strengthen or highlight " + missing_skills[0] + " to improve the match."

        top_missing = ", ".join(missing_skills[:3])
        return "Strengthen or highlight these skills to improve the match: " + top_missing + "."

    def _normalize_questions(self, raw_questions, requested_count, skills, title):
        cleaned_questions = []

        if isinstance(raw_questions, list):
            for raw_question in raw_questions:
                if not isinstance(raw_question, dict):
                    continue

                question_text = str(raw_question.get("question") or "").strip()
                raw_options = raw_question.get("options")
                if not question_text or not isinstance(raw_options, list):
                    continue

                options = []
                for option in raw_options[:4]:
                    option_text = str(option).strip()
                    if option_text:
                        options.append(option_text)

                if len(options) != 4:
                    continue

                try:
                    correct_option_index = int(raw_question.get("correctOptionIndex", 0))
                except (TypeError, ValueError):
                    correct_option_index = 0

                if correct_option_index < 0 or correct_option_index > 3:
                    correct_option_index = 0

                cleaned_questions.append({
                    "question": question_text,
                    "options": options,
                    "correctOptionIndex": correct_option_index
                })

                if len(cleaned_questions) >= requested_count:
                    break

        if len(cleaned_questions) < requested_count:
            raise RuntimeError("Quiz generation failed")

        return cleaned_questions[:requested_count]



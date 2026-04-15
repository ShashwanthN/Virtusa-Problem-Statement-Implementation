from fastapi import APIRouter, File, Form, UploadFile
from typing import Dict, Any
from services import PythonBackendService

router = APIRouter()

backend_service = PythonBackendService()



@router.get("/api/jobs/indexed-ids")
async def get_indexed_job_ids():
    return {"jobIds": backend_service.get_indexed_job_ids()}

@router.post("/api/quiz/generate")
async def generate_quiz(payload: Dict[str, Any]):
    return backend_service.generate_quiz(payload)

@router.post("/api/match")
async def match_resume(file: UploadFile = File(...)):
    file_content = await file.read()
    return backend_service.match_resume(file_content)

@router.post("/api/match/analyze")
async def analyze_match(
    file: UploadFile = File(...),
    job_title: str = Form(...),
    job_description: str = Form(...),
    skills_needed: str = Form(...)
):
    file_content = await file.read()
    return backend_service.analyze_job_match(file_content, {
        "title": job_title,
        "description": job_description,
        "skillsNeeded": skills_needed
    })

@router.post("/api/jobs/index")
async def index_job(payload: Dict[str, Any]):
    backend_service.index_job(payload)
    status_response = {"status": "ok"}
    return status_response

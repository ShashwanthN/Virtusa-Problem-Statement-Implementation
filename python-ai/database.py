class Database:
    jobs = dict()

    @staticmethod
    def save_job(job_id, payload):
        Database.jobs[job_id] = payload

    @staticmethod
    def save_job_if_missing(job_id, payload):
        if job_id not in Database.jobs:
            Database.jobs[job_id] = payload

    @staticmethod
    def get_job(job_id):
        return Database.jobs.get(job_id)

    @staticmethod
    def get_all_jobs():
        return Database.jobs

    @staticmethod
    def get_all_job_ids():
        return list(Database.jobs.keys())

db_instance = Database()

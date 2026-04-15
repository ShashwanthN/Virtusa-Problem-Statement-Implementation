const API_BASE = 'http://localhost:8080/api';

let activeTestJobId = null;
let previousSection = 'sec-match-jobs';
let tempJobData = {};
let questionCount = 0;
let matchedResults = [];

document.addEventListener('DOMContentLoaded', () => {
    nav('sec-match-jobs', document.getElementById('nav-match'));
    document.getElementById('job-details-form').addEventListener('submit', handleStep1);
    document.getElementById('match-resume-form').addEventListener('submit', uploadResumeAndMatch);
    document.getElementById('submit-test-form').addEventListener('submit', submitTest);
});

function nav(sectionId, btn) {
    document.querySelectorAll('.nav-btn').forEach((button) => button.classList.remove('active'));
    if (btn) btn.classList.add('active');

    document.querySelectorAll('.view-section').forEach((section) => section.classList.add('hidden'));
    document.getElementById(sectionId).classList.remove('hidden');

    if (sectionId !== 'sec-take-test') previousSection = sectionId;
}

function navBack() {
    const buttonId = previousSection === 'sec-all-jobs'
        ? 'nav-all'
        : previousSection === 'sec-create-job'
            ? 'nav-create'
            : 'nav-match';
    nav(previousSection, document.getElementById(buttonId));
}

function handleStep1(event) {
    event.preventDefault();

    tempJobData = {
        title: document.getElementById('j-title').value.trim(),
        description: document.getElementById('j-desc').value.trim(),
        skillsNeeded: document.getElementById('j-skills').value.trim(),
        minExperienceYears: Number(document.getElementById('j-exp').value),
        companyId: document.getElementById('j-cid').value.trim(),
        companyName: document.getElementById('j-cname').value.trim(),
        employmentType: document.getElementById('j-etype').value.trim(),
        location: document.getElementById('j-loc').value.trim(),
        minSalary: Number(document.getElementById('j-minsal').value),
        maxSalary: Number(document.getElementById('j-maxsal').value)
    };

    document.getElementById('wizard-step-1').classList.add('hidden');
    document.getElementById('wizard-step-2').classList.remove('hidden');

    if (!document.querySelector('#manual-questions-container .q-block')) addBlankQuestion();
}

function backToJobDetails() {
    document.getElementById('wizard-step-2').classList.add('hidden');
    document.getElementById('wizard-step-1').classList.remove('hidden');
}

function addBlankQuestion(question = '', options = ['', '', '', ''], correctOptionIndex = 0) {
    questionCount += 1;

    const block = document.createElement('div');
    block.className = 'q-block';
    block.innerHTML = `
        <p><strong>Question ${questionCount}</strong></p>
        <label>
            Question
            <input type="text" class="q-text" value="${escapeHtml(question)}" required>
        </label>
        <label>
            Option 1
            <input type="text" class="q-o1" value="${escapeHtml(options[0] || '')}" required>
        </label>
        <label>
            Option 2
            <input type="text" class="q-o2" value="${escapeHtml(options[1] || '')}" required>
        </label>
        <label>
            Option 3
            <input type="text" class="q-o3" value="${escapeHtml(options[2] || '')}" required>
        </label>
        <label>
            Option 4
            <input type="text" class="q-o4" value="${escapeHtml(options[3] || '')}" required>
        </label>
        <label>
            Correct Option Index
            <input type="number" class="q-correct" min="0" max="3" value="${Number(correctOptionIndex) || 0}" required>
        </label>
    `;

    document.getElementById('manual-questions-container').appendChild(block);
}

async function generateAiQuiz() {
    const requestedCount = Number(document.getElementById('ai-qnum').value);
    if (requestedCount < 1) return alert('Enter a valid number');

    try {
        const response = await fetch(`${API_BASE}/jobs/generate-quiz`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ ...tempJobData, numQuestions: requestedCount })
        });

        const payload = await response.json();
        const questions = Array.isArray(payload) ? payload : payload.questions || [];
        if (!questions.length) throw new Error('No questions returned');

        document.getElementById('manual-questions-container').innerHTML = '';
        questionCount = 0;
        questions.forEach((item) => addBlankQuestion(item.question, item.options, item.correctOptionIndex));
    } catch (error) {
        alert(error.message);
    }
}

function collectQuestions() {
    return Array.from(document.querySelectorAll('#manual-questions-container .q-block'))
        .map((block) => ({
            question: block.querySelector('.q-text').value.trim(),
            options: [
                block.querySelector('.q-o1').value.trim(),
                block.querySelector('.q-o2').value.trim(),
                block.querySelector('.q-o3').value.trim(),
                block.querySelector('.q-o4').value.trim()
            ],
            correctOptionIndex: Number(block.querySelector('.q-correct').value)
        }))
        .filter((question) => question.question);
}

async function finalizeJob() {
    const questions = collectQuestions();
    if (!questions.length) return alert('Add at least one question');

    const requiredCorrectAnswers = Math.max(
        1,
        Math.min(Number(document.getElementById('required-correct-answers').value) || 1, questions.length)
    );

    try {
        const response = await fetch(`${API_BASE}/jobs`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                ...tempJobData,
                totalTimeMinutes: Number(document.getElementById('q-time').value),
                requiredCorrectAnswers,
                numQuestions: questions.length,
                questions
            })
        });
        if (!response.ok) throw new Error('Could not save');

        const job = await response.json();
        alert(`Job ${job.id} saved`);
        resetCreateForm();
    } catch (error) {
        alert(error.message);
    }
}

function resetCreateForm() {
    document.getElementById('job-details-form').reset();
    document.getElementById('j-exp').value = '2';
    document.getElementById('j-etype').value = 'FT';
    document.getElementById('j-loc').value = 'Remote';
    document.getElementById('j-minsal').value = '50000';
    document.getElementById('j-maxsal').value = '100000';
    document.getElementById('q-time').value = '30';
    document.getElementById('required-correct-answers').value = '1';
    document.getElementById('ai-qnum').value = '5';
    document.getElementById('manual-questions-container').innerHTML = '';
    document.getElementById('wizard-step-2').classList.add('hidden');
    document.getElementById('wizard-step-1').classList.remove('hidden');

    tempJobData = {};
    questionCount = 0;
}

async function loadAllJobs() {
    const container = document.getElementById('all-jobs-container');
    container.textContent = 'Loading jobs...';

    try {
        const response = await fetch(`${API_BASE}/jobs`);
        if (!response.ok) throw new Error('Fetch failed');

        const jobs = await response.json();
        container.innerHTML = jobs.length
            ? jobs.map((job) => `
                <div class="list-item">
                    <p><strong>${escapeHtml(job.title)}</strong></p>
                    <p>Skills: ${escapeHtml(job.skillsNeeded || 'None')}</p>
                    <p>Location: ${escapeHtml(job.location || 'None')}</p>
                    <div class="section-row">
                        <button type="button" onclick='startJobApplication(${job.id}, ${JSON.stringify(job.title)})'>Apply</button>
                        <button type="button" onclick='analyzeMatch(${job.id})'>Analysis</button>
                    </div>
                </div>
            `).join('')
            : '<p>No jobs found</p>';
    } catch (error) {
        container.textContent = '';
        alert(error.message);
    }
}

async function uploadResumeAndMatch(event) {
    event.preventDefault();

    const file = document.getElementById('apply-resume').files[0];
    if (!file) return alert('Choose PDF');

    const container = document.getElementById('matched-jobs-container');
    const analysis = document.getElementById('match-analysis-container');
    container.textContent = 'Loading matches...';
    analysis.classList.add('hidden');
    analysis.innerHTML = '';

    const formData = new FormData();
    formData.append('file', file);

    try {
        const response = await fetch(`${API_BASE}/jobs/apply`, {
            method: 'POST',
            body: formData
        });
        if (!response.ok) throw new Error('Matching failed');

        matchedResults = ((await response.json()).results || []).sort((left, right) => right.matchScore - left.matchScore);
        renderMatchedJobs();
    } catch (error) {
        container.textContent = '';
        alert(error.message);
    }
}

function renderMatchedJobs() {
    const container = document.getElementById('matched-jobs-container');

    container.innerHTML = matchedResults.length
        ? matchedResults.map((match) => `
            <div class="list-item">
                <p><strong>${escapeHtml(match.job.title)}</strong></p>
                <p>Score: ${match.matchScore}%</p>
                <p>Matched Skills: ${escapeHtml(formatList(match.matchedSkills))}</p>
                <div class="section-row">
                    <button type="button" onclick='startJobApplication(${match.job.id}, ${JSON.stringify(match.job.title)})'>Apply</button>
                    <button type="button" onclick='analyzeMatch(${match.job.id})'>Analysis</button>
                </div>
            </div>
        `).join('')
        : '<p>No jobs found</p>';
}

async function analyzeMatch(jobId) {
    const file = document.getElementById('apply-resume').files[0];
    if (!file) return alert('Choose PDF');

    const match = matchedResults.find((item) => item.job.id === jobId);
    const container = document.getElementById('match-analysis-container');
    const matchedJobsContainer = document.getElementById('matched-jobs-container');

    matchedJobsContainer.classList.add('hidden');
    container.classList.remove('hidden');
    container.innerHTML = '<p>Loading analysis...</p>';

    const formData = new FormData();
    formData.append('file', file);

    try {
        const response = await fetch(`${API_BASE}/jobs/${jobId}/match-analysis`, {
            method: 'POST',
            body: formData
        });
        if (!response.ok) throw new Error('Analysis failed');

        const analysis = await response.json();
        container.innerHTML = `
            <h3>${escapeHtml(match?.job?.title || analysis.job?.title || 'Analysis')}</h3>
            <p><strong>Score:</strong> ${escapeHtml(String(analysis.matchScore ?? match?.matchScore ?? 0))}%</p>
            <p><strong>Summary:</strong> ${escapeHtml(analysis.analysisSummary || 'No summary')}</p>
            <p><strong>Matched Skills:</strong> ${escapeHtml(formatList(analysis.matchedSkills))}</p>
            <p><strong>Missing Skills:</strong> ${escapeHtml(formatList(analysis.missingSkills))}</p>
            <p><strong>Recommendation:</strong> ${escapeHtml(analysis.recommendation || 'No recommendation')}</p>
            <p><strong>LLM Suggestion:</strong> ${escapeHtml(analysis.llmSuggestion || 'No suggestion')}</p>
            <button type="button" onclick="backToMatches()">Back to Matches</button>
        `;
    } catch (error) {
        container.innerHTML = `<p>${escapeHtml(error.message)}</p>`;
    }
}

async function startJobApplication(jobId, jobTitle) {
    activeTestJobId = jobId;
    document.getElementById('test-job-title').textContent = `Application: ${jobTitle}`;
    document.getElementById('questions-list').innerHTML = '';

    document.querySelectorAll('.view-section').forEach((section) => section.classList.add('hidden'));
    document.getElementById('sec-take-test').classList.remove('hidden');

    try {
        const response = await fetch(`${API_BASE}/jobs/${jobId}/start-test`, { method: 'POST' });
        if (!response.ok) throw new Error('Could not load quiz');

        const payload = await response.json();
        const questions = payload.questions || [];

        document.getElementById('test-time-limit').textContent = payload.totalTimeMinutes || 0;
        document.getElementById('test-pass-threshold').textContent = payload.requiredCorrectAnswers || 0;
        document.getElementById('questions-list').innerHTML = questions.length
            ? questions.map((question, index) => `
                <div class="question-item">
                    <p><strong>${index + 1}.</strong> ${escapeHtml(question.question)}</p>
                    ${(question.options || []).map((option, optionIndex) => `
                        <label>
                            <input type="radio" name="q-${index}" value="${optionIndex}" required>
                            ${escapeHtml(option)}
                        </label>
                    `).join('')}
                </div>
            `).join('')
            : '<p>No questions found</p>';
    } catch (error) {
        alert(error.message);
    }
}

async function submitTest(event) {
    event.preventDefault();

    const answers = Array.from(document.querySelectorAll('#questions-list .question-item'))
        .map((_, index) => document.querySelector(`input[name="q-${index}"]:checked`)?.value)
        .map((value) => value === undefined ? -1 : Number(value));

    try {
        const response = await fetch(`${API_BASE}/jobs/${activeTestJobId}/submit-test`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ answers })
        });
        if (!response.ok) throw new Error('Submission failed');

        const result = await response.json();
        alert(result.passed ? 'Applied' : `Not applied. Score: ${result.correctAnswers}/${result.totalQuestions}`);
    } catch (error) {
        alert(error.message);
    }
}

function formatList(items) {
    return Array.isArray(items) && items.length ? items.join(', ') : 'None';
}

function backToMatches() {
    document.getElementById('matched-jobs-container').classList.remove('hidden');
    document.getElementById('match-analysis-container').classList.add('hidden');
}

function escapeHtml(value) {
    return String(value ?? '').replace(/[&<>"']/g, (char) => ({
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#39;'
    }[char]));
}

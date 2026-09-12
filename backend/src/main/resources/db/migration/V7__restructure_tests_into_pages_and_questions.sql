-- Restructure "tests" from a single question (one language/instructions/starter_code/duration) into a
-- container of pages, each with its own timer, each holding one or more questions of type CODE, TEXT,
-- or MULTIPLE_CHOICE. Existing data is migrated forward, not discarded: every existing test becomes a
-- single page with a single CODE question carrying its old language/starter_code, and every already-
-- submitted single-code answer is copied into the new per-question answer table.

CREATE TABLE test_pages (
    id BIGSERIAL PRIMARY KEY,
    test_id BIGINT NOT NULL REFERENCES tests(id) ON DELETE CASCADE,
    page_order INTEGER NOT NULL,
    duration_minutes INTEGER NOT NULL,
    UNIQUE (test_id, page_order)
);

CREATE TABLE test_questions (
    id BIGSERIAL PRIMARY KEY,
    page_id BIGINT NOT NULL REFERENCES test_pages(id) ON DELETE CASCADE,
    question_order INTEGER NOT NULL,
    question_type VARCHAR(20) NOT NULL,
    prompt TEXT NOT NULL,
    language VARCHAR(20),
    starter_code TEXT,
    editor_font_size INTEGER,
    editor_font_color VARCHAR(20),
    UNIQUE (page_id, question_order)
);

CREATE TABLE test_question_options (
    id BIGSERIAL PRIMARY KEY,
    question_id BIGINT NOT NULL REFERENCES test_questions(id) ON DELETE CASCADE,
    option_order INTEGER NOT NULL,
    option_text TEXT NOT NULL,
    is_correct BOOLEAN NOT NULL DEFAULT false,
    UNIQUE (question_id, option_order)
);

CREATE INDEX idx_test_pages_test ON test_pages(test_id);
CREATE INDEX idx_test_questions_page ON test_questions(page_id);
CREATE INDEX idx_test_question_options_question ON test_question_options(question_id);

-- assignment_answers replaces test_assignments.submitted_code with one row per question answered.
-- question_id/selected_option_id are ON DELETE SET NULL (not CASCADE) and the *_snapshot columns are
-- filled in at submit time, so a candidate's historical answer keeps displaying correctly on the review
-- page even if a recruiter later edits the test and that exact question/option no longer exists.
CREATE TABLE assignment_answers (
    id BIGSERIAL PRIMARY KEY,
    assignment_id BIGINT NOT NULL REFERENCES test_assignments(id) ON DELETE CASCADE,
    question_id BIGINT REFERENCES test_questions(id) ON DELETE SET NULL,
    answer_text TEXT,
    selected_option_id BIGINT REFERENCES test_question_options(id) ON DELETE SET NULL,
    prompt_snapshot TEXT,
    question_type_snapshot VARCHAR(20),
    language_snapshot VARCHAR(20),
    selected_option_text_snapshot TEXT,
    selected_option_was_correct_snapshot BOOLEAN
);

CREATE INDEX idx_assignment_answers_assignment ON assignment_answers(assignment_id);

-- Wrap every existing test into page 1 / question 1, carrying over language + starter_code as-is.
-- instructions_html is copied verbatim into prompt for now (it may still contain HTML markup for
-- tests recruiters authored before this migration) -- the 3 system-seeded tests get a clean plain-text
-- rewrite immediately below since their exact content is known.
INSERT INTO test_pages (test_id, page_order, duration_minutes)
SELECT id, 1, duration_minutes FROM tests;

INSERT INTO test_questions (page_id, question_order, question_type, prompt, language, starter_code)
SELECT p.id, 1, 'CODE', t.instructions_html, t.language, t.starter_code
FROM test_pages p JOIN tests t ON t.id = p.test_id;

-- Copy any already-submitted single-code answers into the new per-question answer table (each test has
-- exactly one page/question at this point in the migration, so the join below is unambiguous).
INSERT INTO assignment_answers (assignment_id, question_id, answer_text, prompt_snapshot, question_type_snapshot, language_snapshot)
SELECT ta.id, tq.id, ta.submitted_code, tq.prompt, tq.question_type, tq.language
FROM test_assignments ta
JOIN test_pages tp ON tp.test_id = ta.test_id
JOIN test_questions tq ON tq.page_id = tp.id
WHERE ta.submitted_code IS NOT NULL;

-- Clean plain-text rewrite of the 3 system-seeded tests' prompts (previously HTML).
UPDATE test_questions
SET prompt = $prompt$As part of our technical evaluation, please complete the short Java coding exercise below.

Instructions:
- Please spend approximately 10 minutes on this exercise.
- Review the existing code and address each TODO.
- Modify the code directly rather than only describing the changes.
- Keep the solution simple and readable.
- You may add small helper logic if necessary, but please do not completely rewrite the solution.
- Return the corrected code.

Assume `OrderItem` contains `getPrice()` and `getQuantity()`, and `Order` contains `getItems()` and `getCustomerType()`.

Example: a PREMIUM customer with $100 x 2 and $50 x 1 should produce an expected total of $225.

Please return the corrected implementation.$prompt$
FROM test_pages tp, tests t
WHERE test_questions.page_id = tp.id AND tp.test_id = t.id AND t.title = 'Order Total Calculation';

UPDATE test_questions
SET prompt = $prompt$As part of our technical evaluation, please complete the short Python coding exercise below.

Instructions:
- Please spend approximately 10 minutes on this exercise.
- Review the existing function and address each TODO.
- Modify the code directly rather than only describing the changes.
- Keep the solution simple and readable.
- Standard Python functionality is sufficient; no external libraries are required.
- Return the corrected function.

Test data:
`transactions = [{"customer": "Alice", "amount": 50, "status": "SUCCESS"}, {"customer": "Bob", "amount": 300, "status": "SUCCESS"}, {"customer": "Alice", "amount": 150, "status": "SUCCESS"}, {"customer": "John", "amount": 500, "status": "FAILED"}]`

Expected: `[("Bob", 300), ("Alice", 200)]`

Please return the corrected function.$prompt$
FROM test_pages tp, tests t
WHERE test_questions.page_id = tp.id AND tp.test_id = t.id AND t.title = 'Customer Transaction Totals';

UPDATE test_questions
SET prompt = $prompt$As part of our technical evaluation, please complete the short SQL coding exercise below.

Instructions:
- Please spend approximately 10 minutes on this exercise.
- Review the existing query and address each TODO.
- Modify the SQL directly rather than only describing the issues.
- Keep the solution simple and readable.
- Standard SQL, or the syntax for the relational database you are most comfortable with, is acceptable.
- Return the corrected query.

Tables: `CUSTOMERS(customer_id, customer_name)` and `ORDERS(order_id, customer_id, status, amount)`.

Expected result:
`John | 3 orders | 1200`, `Sarah | 1 order | 500`, `Mike | 0 orders | 0`.

Please return the corrected query.$prompt$
FROM test_pages tp, tests t
WHERE test_questions.page_id = tp.id AND tp.test_id = t.id AND t.title = 'Completed Orders Report';

ALTER TABLE tests
    DROP COLUMN instructions_html,
    DROP COLUMN starter_code,
    DROP COLUMN language,
    DROP COLUMN duration_minutes;

ALTER TABLE test_assignments DROP COLUMN submitted_code;

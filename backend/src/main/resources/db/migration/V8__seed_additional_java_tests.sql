-- 3 additional system-seeded Java tests (junior/mid/senior), each with 3 pages matching the pattern
-- requested: an MCQ page, then a theory page, then a code page. 5 questions total per test.

-- ===================== Junior Java Developer =====================
WITH t AS (
    INSERT INTO tests (title, created_by, active) VALUES ('Junior Java Developer Assessment', NULL, true) RETURNING id
),
p1 AS (
    INSERT INTO test_pages (test_id, page_order, duration_minutes) SELECT id, 1, 5 FROM t RETURNING id
),
p2 AS (
    INSERT INTO test_pages (test_id, page_order, duration_minutes) SELECT id, 2, 5 FROM t RETURNING id
),
p3 AS (
    INSERT INTO test_pages (test_id, page_order, duration_minutes) SELECT id, 3, 10 FROM t RETURNING id
),
q1 AS (
    INSERT INTO test_questions (page_id, question_order, question_type, prompt)
    SELECT id, 1, 'MULTIPLE_CHOICE', 'Which of the following is a primitive type in Java?' FROM p1 RETURNING id
),
q2 AS (
    INSERT INTO test_questions (page_id, question_order, question_type, prompt)
    SELECT id, 2, 'MULTIPLE_CHOICE', 'What does `==` compare when used on two `String` objects created with `new String(...)`?' FROM p1 RETURNING id
),
q3 AS (
    INSERT INTO test_questions (page_id, question_order, question_type, prompt)
    SELECT id, 3, 'MULTIPLE_CHOICE', 'Which access modifier makes a class member accessible only within its own declaring class?' FROM p1 RETURNING id
),
q4 AS (
    INSERT INTO test_questions (page_id, question_order, question_type, prompt)
    SELECT id, 1, 'TEXT', $prompt$In your own words, explain the difference between method overloading and method overriding in Java. Give a one-line example of each.$prompt$ FROM p2 RETURNING id
),
q5 AS (
    INSERT INTO test_questions (page_id, question_order, question_type, prompt, language, starter_code)
    SELECT id, 1, 'CODE',
        $prompt$Write a method `addTwoNumbers` that takes two integers and returns their sum.$prompt$,
        'JAVA',
        $code$public int addTwoNumbers(int a, int b) {
    // TODO: return the sum of a and b
    return 0;
}$code$
    FROM p3 RETURNING id
),
o1 AS (
    INSERT INTO test_question_options (question_id, option_order, option_text, is_correct)
    SELECT id, 1, 'int', true FROM q1
    UNION ALL SELECT id, 2, 'Integer', false FROM q1
    UNION ALL SELECT id, 3, 'String', false FROM q1
    UNION ALL SELECT id, 4, 'ArrayList', false FROM q1
),
o2 AS (
    INSERT INTO test_question_options (question_id, option_order, option_text, is_correct)
    SELECT id, 1, 'Object references (whether the two point to the same instance)', true FROM q2
    UNION ALL SELECT id, 2, 'The string contents only', false FROM q2
    UNION ALL SELECT id, 3, 'It always evaluates to true for equal-looking strings', false FROM q2
    UNION ALL SELECT id, 4, 'It causes a compile error', false FROM q2
),
o3 AS (
    INSERT INTO test_question_options (question_id, option_order, option_text, is_correct)
    SELECT id, 1, 'private', true FROM q3
    UNION ALL SELECT id, 2, 'public', false FROM q3
    UNION ALL SELECT id, 3, 'protected', false FROM q3
    UNION ALL SELECT id, 4, 'default (package-private)', false FROM q3
)
SELECT 1;

-- ===================== Mid-Level Java Developer =====================
WITH t AS (
    INSERT INTO tests (title, created_by, active) VALUES ('Mid-Level Java Developer Assessment', NULL, true) RETURNING id
),
p1 AS (
    INSERT INTO test_pages (test_id, page_order, duration_minutes) SELECT id, 1, 5 FROM t RETURNING id
),
p2 AS (
    INSERT INTO test_pages (test_id, page_order, duration_minutes) SELECT id, 2, 5 FROM t RETURNING id
),
p3 AS (
    INSERT INTO test_pages (test_id, page_order, duration_minutes) SELECT id, 3, 15 FROM t RETURNING id
),
q1 AS (
    INSERT INTO test_questions (page_id, question_order, question_type, prompt)
    SELECT id, 1, 'MULTIPLE_CHOICE', 'What is the time complexity of retrieving an element by index from a `LinkedList` in Java?' FROM p1 RETURNING id
),
q2 AS (
    INSERT INTO test_questions (page_id, question_order, question_type, prompt)
    SELECT id, 2, 'MULTIPLE_CHOICE', 'What happens when you put a null key into a `HashMap` in Java?' FROM p1 RETURNING id
),
q3 AS (
    INSERT INTO test_questions (page_id, question_order, question_type, prompt)
    SELECT id, 3, 'MULTIPLE_CHOICE', 'Which of these is a checked exception in Java?' FROM p1 RETURNING id
),
q4 AS (
    INSERT INTO test_questions (page_id, question_order, question_type, prompt)
    SELECT id, 1, 'TEXT', $prompt$Describe a scenario where you would prefer composition over inheritance when designing a Java class, and explain why.$prompt$ FROM p2 RETURNING id
),
q5 AS (
    INSERT INTO test_questions (page_id, question_order, question_type, prompt, language, starter_code)
    SELECT id, 1, 'CODE',
        $prompt$Given a `List<Order>` where each `Order` has `getAmount()` and `getStatus()`, write a method `totalCompletedAmount` that returns the sum of amounts for orders with status "COMPLETED". Return 0 if the list is null or empty.$prompt$,
        'JAVA',
        $code$public double totalCompletedAmount(List<Order> orders) {
    // TODO: sum the amount of all orders with status "COMPLETED"
    // Return 0 if orders is null or empty
    return 0;
}$code$
    FROM p3 RETURNING id
),
o1 AS (
    INSERT INTO test_question_options (question_id, option_order, option_text, is_correct)
    SELECT id, 1, 'O(n)', true FROM q1
    UNION ALL SELECT id, 2, 'O(1)', false FROM q1
    UNION ALL SELECT id, 3, 'O(log n)', false FROM q1
    UNION ALL SELECT id, 4, 'O(n^2)', false FROM q1
),
o2 AS (
    INSERT INTO test_question_options (question_id, option_order, option_text, is_correct)
    SELECT id, 1, 'It is allowed -- HashMap supports one null key', true FROM q2
    UNION ALL SELECT id, 2, 'It throws a NullPointerException', false FROM q2
    UNION ALL SELECT id, 3, 'It throws an IllegalArgumentException', false FROM q2
    UNION ALL SELECT id, 4, 'It is silently ignored', false FROM q2
),
o3 AS (
    INSERT INTO test_question_options (question_id, option_order, option_text, is_correct)
    SELECT id, 1, 'IOException', true FROM q3
    UNION ALL SELECT id, 2, 'NullPointerException', false FROM q3
    UNION ALL SELECT id, 3, 'ArrayIndexOutOfBoundsException', false FROM q3
    UNION ALL SELECT id, 4, 'IllegalStateException', false FROM q3
)
SELECT 1;

-- ===================== Senior Java Developer =====================
WITH t AS (
    INSERT INTO tests (title, created_by, active) VALUES ('Senior Java Developer Assessment', NULL, true) RETURNING id
),
p1 AS (
    INSERT INTO test_pages (test_id, page_order, duration_minutes) SELECT id, 1, 5 FROM t RETURNING id
),
p2 AS (
    INSERT INTO test_pages (test_id, page_order, duration_minutes) SELECT id, 2, 8 FROM t RETURNING id
),
p3 AS (
    INSERT INTO test_pages (test_id, page_order, duration_minutes) SELECT id, 3, 20 FROM t RETURNING id
),
q1 AS (
    INSERT INTO test_questions (page_id, question_order, question_type, prompt)
    SELECT id, 1, 'MULTIPLE_CHOICE', 'What does declaring a field as `volatile` guarantee in Java?' FROM p1 RETURNING id
),
q2 AS (
    INSERT INTO test_questions (page_id, question_order, question_type, prompt)
    SELECT id, 2, 'MULTIPLE_CHOICE', 'Compared to a `HashMap` wrapped with `Collections.synchronizedMap`, what advantage does `ConcurrentHashMap` typically offer?' FROM p1 RETURNING id
),
q3 AS (
    INSERT INTO test_questions (page_id, question_order, question_type, prompt)
    SELECT id, 3, 'MULTIPLE_CHOICE', 'Which statement about Java garbage collection is correct?' FROM p1 RETURNING id
),
q4 AS (
    INSERT INTO test_questions (page_id, question_order, question_type, prompt)
    SELECT id, 1, 'TEXT', $prompt$You're designing a shared in-memory cache accessed by multiple threads with frequent reads and occasional writes. Walk through how you'd approach making it thread-safe, and the trade-offs of the approach you'd pick.$prompt$ FROM p2 RETURNING id
),
q5 AS (
    INSERT INTO test_questions (page_id, question_order, question_type, prompt, language, starter_code)
    SELECT id, 1, 'CODE',
        $prompt$Implement thread-safe `increment()` and `get()` methods on the counter below, using whatever concurrency primitive you think is appropriate.$prompt$,
        'JAVA',
        $code$public class SafeCounter {
    private int count = 0;

    public void increment() {
        // TODO: implement thread-safe increment
    }

    public int get() {
        // TODO: implement thread-safe read
        return count;
    }
}$code$
    FROM p3 RETURNING id
),
o1 AS (
    INSERT INTO test_question_options (question_id, option_order, option_text, is_correct)
    SELECT id, 1, 'Visibility of writes across threads, not atomicity of compound operations', true FROM q1
    UNION ALL SELECT id, 2, 'Full atomicity for all operations on the field', false FROM q1
    UNION ALL SELECT id, 3, 'The field becomes thread-local', false FROM q1
    UNION ALL SELECT id, 4, 'The field cannot be changed after initialization', false FROM q1
),
o2 AS (
    INSERT INTO test_question_options (question_id, option_order, option_text, is_correct)
    SELECT id, 1, 'Finer-grained locking allowing higher concurrent throughput', true FROM q2
    UNION ALL SELECT id, 2, 'It disables concurrent reads entirely', false FROM q2
    UNION ALL SELECT id, 3, 'It guarantees insertion order', false FROM q2
    UNION ALL SELECT id, 4, 'It uses less memory in all cases', false FROM q2
),
o3 AS (
    INSERT INTO test_question_options (question_id, option_order, option_text, is_correct)
    SELECT id, 1, 'Most collectors treat the majority of objects as short-lived and collect the young generation more frequently', true FROM q3
    UNION ALL SELECT id, 2, 'The GC guarantees objects are collected the instant they become unreachable', false FROM q3
    UNION ALL SELECT id, 3, 'Explicitly calling System.gc() forces an immediate collection', false FROM q3
    UNION ALL SELECT id, 4, 'Garbage collection is disabled by default and must be turned on', false FROM q3
)
SELECT 1;

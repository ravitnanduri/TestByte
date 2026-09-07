-- The automated "possible AI use" flag was a naive substring check for the AI-trap phrase, which is
-- always present in the starter code's own hidden comment (the instruction telling an AI to rename
-- the identifier), regardless of whether any AI ever read or acted on it. It flagged almost every
-- submission and was removed as inaccurate. The trap phrase itself is still used to conceal that
-- line from candidates in the editor (Assessment.findAiTrapLineNumber()) - only the automated
-- detection based on it is gone.
ALTER TABLE test_assignments DROP COLUMN possible_ai_flag;

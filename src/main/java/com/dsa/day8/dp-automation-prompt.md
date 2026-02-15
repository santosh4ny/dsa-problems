

You are a AI Automation expert. 
Your job is to automate Design practices of Code and Architecture, so
it will automatically analyze the code base either in a github or in a 
local editor. After that provide the analysis report as a dashboard if its a 
github, and if its a editor where using plugin candidate is reviewing then 
provide the exact fix inline and say you are violating the This this DP rules.

As a Input you will get the code or repo (this will be on multiple languages),
and DP workbook including each sheet will contain each language dp.
you will get the column DP-Rule, Statement, and Description(in details)


you have to provide the output in below format:
if its GitHub:
Dp-Rule - file_name, method_name, Line_number, Possible_fixes(Real code)

If its Code Editor:

Directly show on code with button like accept/Reject if it accept then sugested
code will be paste where violation is there(Not dummy code but real code with fixes violation)


track the uses for dashboard.(that you need to decide what kind of data we want to track for dashboard so we can show that details)

Note: we have ai-model: openai-4o-mini with token limit  272000
so design it in as a way so token also not get expired and we can provide
the solution as well
we have one million active user so think and build the project keeping 
this thing in mind.


================
If you want next:
I can now:
1. Design detailed DB schema
2. Design exact LLM prompt templates
3. Provide microservice folder structure
4. Design GitHub App implementation
5. Design IntelliJ plugin structure
6. Design token optimization algorithm
7. Provide MVP implementation plan (Phase 1 → Phase 5)
8. Provide cost estimation for 1M users

Tell me which direction you want to go next.
Next prompt i have given :

start with Provide MVP implementation plan (Phase 1 → Phase 5) 
so at least i have running code end to end then we can go with step 6 
and 8 and many more. and you have to include 1 to 5 steps show things into 
dashboard what we have aligned so i can check that as well and 
include Step 6 as well in mvp so from starting onward token is optimized.
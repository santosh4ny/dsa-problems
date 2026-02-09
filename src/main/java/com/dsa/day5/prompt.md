
I am going to create a multi-agent ai model.
Each agent is a specialist in his domain.
Agents
- Agent1: will do static code analysis
  - Agent2: will analyze java code (here general and company guidance like design practices we have to include for all the language.)
  - Agent3: will analyze C code
  - Agent4: will analyze C++ code
  - Agent5: will analyze python code
  - Agent6: will analyze javaScript code
  - Agent7: will analyze dot Net code
- Agent8: Will do design analysis
  - Agent9: like(code analysis agent, here also have multiple agent based on language)
- Agent16: will do Architecture analysis
- Agent17: will do Document analysis
  - Agent18: some company provided document expert.
- Agent21: This agent will receive all the result from each agent which is required.

One Aggregator agent will receive all agent results, 
and it will consolidate it and send back to the front end.

These all agent will work on two platforms 
1. in github/gitlab using webhook
   i. it will scan all the project(code level, design level and architecture level) using all the agenet
2. in plugin (vs Code or intelliJ)
    i. Using plugin we can also track project(code level, design level and architecture level) 
    ii. local development so only coding analysis 

Once aggregator agent will receive all the results then it will analyze and send these data in a 
correct output format so this will be utilized in a dashboard for tracking purpose.

kindly provide details architecture(High level and low level), flow Diagram 
and plan how i can acheive this. i have openai-4o-mini token with max token size is 272000.
Note: in architecture provide details why, tradeoffs etc.


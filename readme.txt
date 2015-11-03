Reflect about your solution!

Summary:

This solution works by using the predefined Shell to process user I/O.
But, it takes it a step further, and abstracts a common superclass, the CommandInterpreter,
    to interpret commands going to / coming from the server in the same way.
This is achieved by handling the commands known to it with the annotated methods,
    unknown commands get passed through to the output stream.
The advantage of this solution is:
    A reusable abstraction of a CommandInterpreter, reading from a stream and executing methods based on the commands.
    Good encapsulation of methods to and from the user.
    Dealing with commands coming from the server in the same way as those comming from the user.
You end up using the CommandInterpreter as Channels to route your communication throug
    and it filters out commands that have to be handled in a special way.

To make this work, I had to make some adjustments to the Shell, and did not use the Interfaces as expected,
    but it still works with the Testing facility and ends up being more elegant, so I hope this is not an issue.

Small note: I did change a little detail in the ScenarioTest class
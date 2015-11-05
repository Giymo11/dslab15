Reflect about your solution!

Summary:

This solution works by using (some components of) the predefined Shell to process user I/O.
But, it takes it a step further, and abstracts a common superclass, the CommandInterpreter,
    to interpret commands going to / coming from the server in the same way.
This is achieved by handling the commands known to it with the annotated methods,
    unknown commands get passed through to the output stream.
The advantage of this solution is:
    A reusable abstraction of a CommandInterpreter, reading from a stream and executing methods based on the commands.
    Good encapsulation of methods to and from the user.
    Dealing with commands coming from the server in the same way as those comming from the user.
You end up using the CommandInterpreter as Channels to route your communication through
    and it filters out commands that have to be handled in a special way.
This is why the writeLine method of the CommandInterpreter had to be made synchronized - to ensure Thread safety.

To make this work, I had to make some adjustments to the Shell, and did not use the Interfaces as expected,
    but it still works with the Testing facility and ends up being more elegant, so I hope this is not an issue.

This enables me to use concurrency very heavily. I made the decision to use the ExecutorService with the Chatserver,
    but not with the Client, to show and explain the difference.

Also, it keeps mutable state to a minimum, because everything gets encapsulated into its own worker Thread,
    helping a lot with Concurrency and enabling this "reactive" architecture.

I chose to implement the Datagram features in a very simple (and mostly serial) way, to be able to demonstrate
    the differences to the architecture I used for the TCP commands.

Concerning the implementation of the !msg command, I chose to follow through with the reactive style,
    not blocking but rather saving the message as "pending" if the user cannot be reached immediately,
    and delivering all messages as soon as the user's address is known to us. To ensure that they are delivered
    in the right order, I use a synchronized block and wait for the users' !acks.

Small note: I did change some little details in the ScenarioTest class, but no cheating,
    just formatting / more options to test.
# Java-UDP-Chat-Room
A chat room made by UDP in Java.

## About the client
The client will generate a log when running and a txt file which contains the chat when closing properly.

Note that *udpclient.json* and *client_logging.properties* is required. And *udpclient.png* is recommended (in order to show the icon).  

Also note that the server will not know the client is exiting if you end the client process in the task manager, so this is **not** recommended.

## About the server
The server will generate a log when running.

If you find a warning at the end of the log, don't worry, that's a bug. 

Also require *udpserver.json* and *server_logging.properties*.

## How to change the config (both the client and the server)
To the client, the config can provide the address of the server and the font. To the server, it can provide the address it uses.   

### To change the address
The default config should be like this:
```
...
"socket": {
    "Host": "localhost",
    "Port": 14514
},
...
```
Just change *Host* and *Port*.
### To change the font
The default is similar to the one above.

Just change *Name* and *Size*.

**Notice: If *Size* is too big, the GUI may glitch.**

### To configure the logger
It's configured in *client_logging.properties* (or *server_logging.properties*)

*No tutorial now!*

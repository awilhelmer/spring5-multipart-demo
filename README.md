# spring5-mutlipart-demo

This project demonstrates to delegate a multipart file request to another endpoint in Spring5. 
The goal is it to do it in a most reactive way, but it have a little blocking part handling the temp files
and filling the webclient request (MultiValueMap)

For testing start the Junit Test in `FileControllerTest`


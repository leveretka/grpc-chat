package com.example.chatclient

import javax.servlet.annotation.WebInitParam
import javax.servlet.annotation.WebServlet

@WebServlet(
        urlPatterns = ["/*"],
        name = "MyServlet",
        displayName = "gRPC Vaadin Client",
        asyncSupported = true,
        loadOnStartup = 1,

        initParams = [WebInitParam(name="ui", value="com.example.chatclient.MainUI")])
class ChatClientServlet : com.vaadin.server.VaadinServlet()

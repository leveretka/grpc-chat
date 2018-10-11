package com.example.chatclient

import com.vaadin.navigator.View
import com.vaadin.server.Page
import com.vaadin.server.VaadinSession
import com.vaadin.ui.*


class LoginPage : VerticalLayout(), View {

    companion object {
        const val NAME = ""
    }

    private val panel = Panel("Login")
    init {
        panel.setSizeUndefined()
        addComponent(panel)
        val content = FormLayout()
        val username = TextField("Username")
        content.addComponent(username)

        val send = Button("Enter")
        send.addClickListener {
            VaadinSession.getCurrent().setAttribute("user", username.value)
            ui.navigator.addView(SecurePage.NAME, SecurePage::class.java)
            Page.getCurrent().uriFragment = "!${SecurePage.NAME}"
        }

        content.run {
            addComponent(send)
            setSizeUndefined()
            setMargin(true)
        }
        panel.content = content
        setComponentAlignment(panel, Alignment.MIDDLE_CENTER)
    }
}
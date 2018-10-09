package com.example.chatclient

import com.vaadin.navigator.View
import com.vaadin.server.Page
import com.vaadin.server.VaadinSession
import com.vaadin.ui.*


class LoginPage : VerticalLayout(), View {

    companion object {
        val NAME = ""
    }

    val panel = Panel("Login")
    init {
        panel.setSizeUndefined()
        addComponent(panel)
        val content = FormLayout()
        val username = TextField("Username")
        content.addComponent(username)

        val send = Button("Enter")
        send.addClickListener(object : Button.ClickListener {
            override fun buttonClick(event: Button.ClickEvent) {
                    VaadinSession.getCurrent().setAttribute("user", username.value)
                    ui.navigator.addView(SecurePage.NAME, SecurePage::class.java)
                    Page.getCurrent().setUriFragment("!" + SecurePage.NAME)
            }

        })

        content.addComponent(send)
        content.setSizeUndefined()
        content.setMargin(true)
        panel.setContent(content)
        setComponentAlignment(panel, Alignment.MIDDLE_CENTER);
    }
}
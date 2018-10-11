package com.example.chatclient

import com.vaadin.annotations.PreserveOnRefresh
import com.vaadin.annotations.Push
import com.vaadin.annotations.Theme
import com.vaadin.navigator.Navigator
import com.vaadin.server.Page
import com.vaadin.server.VaadinRequest
import com.vaadin.shared.ui.ui.Transport
import com.vaadin.ui.Notification
import com.vaadin.ui.UI


@Theme("ChatClient")
@Push(transport = Transport.LONG_POLLING)
@PreserveOnRefresh
class MainUI : UI() {
    override fun init(request: VaadinRequest?) {
        Navigator(this, this)
        navigator.addView(LoginPage.NAME, LoginPage::class.java)

        Page.getCurrent().addPopStateListener { event -> router(event.uri) }
        router("")
    }

    private fun router(route: String) {
        Notification.show(route)
        navigator.run {
            if (session.getAttribute("user") != null) {
                addView(SecurePage.NAME, SecurePage::class.java)
                navigateTo(SecurePage.NAME)
            } else {
                navigateTo(LoginPage.NAME)
            }
        }
    }


}
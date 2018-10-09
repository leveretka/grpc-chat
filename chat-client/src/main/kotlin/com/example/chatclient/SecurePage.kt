package com.example.chatclient

import com.vaadin.navigator.View
import com.vaadin.server.VaadinSession
import com.vaadin.ui.*
import ua.nedz.grpc.ChatProto
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent



class SecurePage : VerticalLayout(), View {
    companion object {
        const val NAME = ""
    }

    private val mainLayout = HorizontalLayout()
    private val chatLayout = VerticalLayout()
    private val voteLayout = VerticalLayout()
    private val message = TextField()
    private val button = Button("Send")
    private val area = TextArea("Chat")
    private var userName = VaadinSession.getCurrent().getAttribute("user").toString()


    private val chatClient = ChatClient()

    init {
        area.rows = 20

        message.caption = "Type your message here:"

        chatLayout.caption = "Chat here"
        voteLayout.caption = "Vote here"

        chatLayout.addComponents(area, message , button)
        mainLayout.addComponents(chatLayout, voteLayout)

        addComponent(mainLayout)
    }

    private fun chat(currentUI: UI) {
        val observer = chatClient.chat {
            currentUI.access {
                area.value = "${area.value}${it.from}: ${it.content}\n"
            }
        }

        button.addClickListener {
            val messageValue = message.value
            observer.onNext(ChatProto.ChatMessage
                    .newBuilder()
                    .setFrom(userName)
                    .setContent(messageValue)
                    .build())
            chatClient.addMessage(userName, messageValue)
        }
    }

    private fun join(currentUI: UI) {
        chatClient.join(userName) { it ->
            currentUI.access {
                voteLayout.removeAllComponents()
                it.recordList.forEach { record ->
                    val author = Label(record.author)
                    val content = Label(record.content)
                    val votes = Label("${record.votes}")
                    val voteBtn = Button("Vote")
                    voteBtn.addClickListener { chatClient.vote(record.id, userName) }

                    val recordLayout = HorizontalLayout()
                    recordLayout.addComponents(author, content, votes, voteBtn)
                    voteLayout.addComponent(recordLayout)
                }
            }
        }
        println("Joined")
    }

    override fun enter(event: ViewChangeEvent?) {
        userName = VaadinSession.getCurrent().getAttribute("user").toString()
        chatLayout.caption = "Hello, $userName!"
        val currentUI = UI.getCurrent()


        join(currentUI)

        chat(currentUI)
    }
}

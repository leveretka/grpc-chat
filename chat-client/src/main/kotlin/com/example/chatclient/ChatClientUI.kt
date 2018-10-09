package com.example.chatclient

import com.vaadin.annotations.PreserveOnRefresh
import com.vaadin.annotations.Push
import com.vaadin.annotations.Theme
import com.vaadin.server.VaadinRequest
import com.vaadin.shared.communication.PushMode
import com.vaadin.ui.*
import ua.nedz.grpc.ChatProto


@Theme("ChatClient")
@Push
@PreserveOnRefresh
class ChatClientUI : UI() {

    private val mainLayout = HorizontalLayout()
    private val chatLayout = VerticalLayout()
    private val voteLayout = VerticalLayout()
    private val name = TextField()
    private val message = TextField()
    private val button = Button("Send")
    private val area = TextArea("Chat")

    private val chatClient = ChatClient()

    init {
        area.rows = 20

        name.id = "tf.name"
        name.caption = "Type your name here:"

        message.id = "tf.message"
        message.caption = "Type your message here:"

        chatLayout.caption = "Chat here"
        voteLayout.caption = "Vote here"

        chatLayout.addComponents(area, name , message , button)
        mainLayout.addComponents(chatLayout, voteLayout)

        content = mainLayout
    }

    override fun init(request: VaadinRequest) {
        println("Inside Init")
        val currentUI = UI.getCurrent()

        chatClient.join(name.value) {
            currentUI.access {
                voteLayout.removeAllComponents()
                it.recordList.forEach { record ->
                    val author = Label(record.author)
                    val content = Label(record.content)
                    val votes  = Label("${record.votes}")
                    val voteBtn = Button("Vote")
                    voteBtn.addClickListener { chatClient.vote(record.id, name.value) }

                    val recordLayout = HorizontalLayout()
                    recordLayout.addComponents(author, content, votes, voteBtn)
                    voteLayout.addComponent(recordLayout)
                }
            }
        }
        println("Joined")

        val observer = chatClient.chat {
            currentUI.access {
                area.value = "${area.value}${it.from}: ${it.content}\n"
            }
        }

        button.addClickListener {
            val nameValue = name.value
            val messageValue = message.value
            observer.onNext(ChatProto.ChatMessage
                    .newBuilder()
                    .setFrom(nameValue)
                    .setContent(messageValue)
                    .build())
            chatClient.addMessage(nameValue, messageValue)
        }

    }
}

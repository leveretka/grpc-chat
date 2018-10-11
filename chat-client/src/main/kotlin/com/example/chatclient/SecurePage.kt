package com.example.chatclient

import com.vaadin.navigator.View
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent
import com.vaadin.server.FileResource
import com.vaadin.server.VaadinService
import com.vaadin.server.VaadinSession
import com.vaadin.ui.*
import ua.nedz.grpc.ChatProto
import java.io.File


class SecurePage : VerticalLayout(), View {
    companion object {
        const val NAME = ""
    }

    private val screen = VerticalLayout()
    private val mainLayout = Accordion()
    private val chatLayout = VerticalLayout()
    private var votingLayout =  GridLayout(4, 1)
    private val message = TextField()
    private val button = Button("Send")
    private val area = TextArea("Chat")
    private var userName = VaadinSession.getCurrent().getAttribute("user").toString()
    private var image = Image()


    private val chatClient = ChatClient()

    init {
        val basepath = VaadinService.getCurrent()
                .baseDirectory.absolutePath

        val resource = FileResource(File(
                "$basepath/WEB-INF/images/logo-preview-680ac74d-1e74-4677-85a2-966a36623b0d.jpg"))

        image = Image("", resource)
        image.setWidth("250px")

        votingLayout.addComponents(Label("Author"), Label("Message"), Label("Likes"),
                Label("Vote"))
        votingLayout.setWidth("100%")
        votingLayout.setHeight("100%")
        message.caption = "Type your message here:"
        votingLayout.caption = "Vote here"
        votingLayout.defaultComponentAlignment = Alignment.MIDDLE_CENTER

        chatLayout.addComponents(area.apply {
            setWidth("100%")
            setHeight("100%")
        }, message.apply { setWidth("100%") } , button)
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
            message.value = ""
        }
    }

    private fun join(currentUI: UI) {
        chatClient.join(userName) { it ->
            currentUI.access {
                votingLayout.run {
                    removeAllComponents()
                    rows = it.recordCount + 1
                    addComponents(Label("Author"), Label("Message"), Label("Likes"),
                            Label("Vote"))
                    it.recordList.forEach { record ->
                        val author = Label(record.author)
                        val content = Label(record.content)
                        val votes = Label("${record.votes}")
                        val voteBtn = Button("+")
                        voteBtn.addClickListener { chatClient.vote(record.id, userName) }
                        addComponents(author, content, votes, voteBtn)
                    }
                }
            }
        }
        println("Joined")
    }

    override fun enter(event: ViewChangeEvent?) {
        userName = VaadinSession.getCurrent().getAttribute("user").toString()
        screen.defaultComponentAlignment = Alignment.TOP_CENTER

        screen.addComponents(image, mainLayout.apply {
            setWidth("100%")
            setHeight("100%")
            addTab(chatLayout.apply { setHeight("100%") }, "Hello, $userName!")
            addTab(votingLayout.apply { setHeight("100%") }, "Let's vote!")
        })

        addComponent(screen)

        val currentUI = UI.getCurrent()
        join(currentUI)
        chat(currentUI)
    }
}

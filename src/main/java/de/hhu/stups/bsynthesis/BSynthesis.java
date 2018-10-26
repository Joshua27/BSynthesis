package de.hhu.stups.bsynthesis

import com.google.inject.Guice
import com.google.inject.Injector

import de.hhu.stups.bsynthesis.injector.BSynthesisModule
import de.hhu.stups.bsynthesis.services.ApplicationEvent
import de.hhu.stups.bsynthesis.services.ApplicationEventType
import de.hhu.stups.bsynthesis.services.UiService
import de.hhu.stups.bsynthesis.ui.controller.SynthesisMain
import de.prob.cli.ProBInstanceProvider

import javafx.application.Application
import javafx.application.Platform
import javafx.scene.Scene
import javafx.stage.Stage

class BSynthesis : Application() {

    private var injector: Injector? = null
    private var uiService: UiService? = null

    override fun start(stage: Stage) {
        System.setProperty("logback.configurationFile", "config/logging.xml")

        injector = Guice.createInjector(BSynthesisModule())

        uiService = injector!!.getInstance<UiService>(UiService::class.java)
        val root = injector!!.getInstance<SynthesisMain>(SynthesisMain::class.java)
        val mainScene = Scene(root, 1024.0, 768.0)
        root.getStylesheets().add("main.css")
        root.setStage(stage)

        stage.title = "BSynthesis"
        stage.scene = mainScene
        stage.setOnCloseRequest { e -> Platform.exit() }

        stage.show()
    }

    /**
     * Shut down all injector instances and send [ApplicationEventType.CLOSE_APP] to the
     * [UiService.applicationEventStream] to shut down all executor services etc.
     */
    override fun stop() {
        injector!!.getInstance<ProBInstanceProvider>(ProBInstanceProvider::class.java).shutdownAll()
        uiService!!.applicationEventStream().push(
                ApplicationEvent(ApplicationEventType.CLOSE_APP))
        Platform.exit()
        // TODO: fix this, the model checker seems to be not closed properly
        System.exit(0)
    }

    companion object {

        @JvmStatic
        fun main(args: Array<String>) {
            Application.launch(*args)
        }
    }
}
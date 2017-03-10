package wop.server

import akka.actor._
import korolev._
import korolev.blazeServer.{defaultExecutor, _}
import korolev.server._
import org.slf4j.LoggerFactory
import wop.server.actors.PlayerActor.Notification._
import wop.server.actors.{MatchMakingActor, PlayerActor}
import wop.server.controller.{EnterNickNameController, InGameController, MatchMakingController}
import wop.server.view.{EnterNickNameView, InGameView, MatchMakingView}

import scala.concurrent.Future
import scala.language.postfixOps

object WopServer extends KorolevBlazeServer {

  val logger = LoggerFactory.getLogger(WopServer.getClass)
  logger.info("Starting Wizards of Portal")

  implicit val system = ActorSystem("wop-server")
  val matchmaking = system.actorOf(MatchMakingActor.props)
  val stateStorage = StateStorage.default[Future, UserState](UserState.EnterNickName(false))

  val service = blazeService[Future, UserState, PlayerActor.Command] from KorolevServiceConfig(
    serverRouter = ServerRouter
      .empty,
      //.withRootPath("/wop/"),
    stateStorage = stateStorage,
    render = {
      val enterNickNameController = new EnterNickNameController()
      val enterNickNameView = new EnterNickNameView(enterNickNameController)
      val inGameController = new InGameController()
      val inGameView = new InGameView(inGameController)
      val matchMakingController = new MatchMakingController()
      val matchMakingView = new MatchMakingView(matchMakingController)

      enterNickNameView.render orElse
        inGameView.render orElse
        matchMakingView.render
    },
    envConfigurator = (deviceId, sessionId, applyTransition) => {
      val actor = {
        val props = PlayerActor.props(matchmaking) {
          case MatchMakingStarted(name, online) =>
            applyTransition { case _ => UserState.Matchmaking(name, online) }
          case GameStated(state, role, name, enemyName) =>
            applyTransition { case _ => UserState.InGame(role, name, enemyName, state) }
          case Error(message) =>
          case Timeout(player) => applyTransition {
            case userState: UserState.InGame =>
              val message = if (userState.yourRole == player) "You didn't do turn" else "Enemy has left the game"
              UserState.GameAborted(message)
          }
          case GameAborted =>
            applyTransition { case _ =>
              UserState.GameAborted("Enemy has left the game")
            }
          case StateUpdated(wopState) =>
            applyTransition {
              case userState: UserState.InGame =>
                userState.copy(wopState = wopState)
            }
        }
        system.actorOf(props)
      }
      KorolevServiceConfig.Env(
        onDestroy = () => {
          actor ! PlayerActor.NotifyDisconnect
          actor ! PoisonPill
        },
        onMessage = {
          case command =>
            actor ! command
        }
      )
    },
    head = 'head (
      'link ('href /= "https://fonts.googleapis.com/css?family=Open+Sans", 'rel /= "stylesheet"),
      'link ('href /= "icons/css/ionicons.min.css", 'rel /= "stylesheet"),
      'link ('href /= "wop.css", 'rel /= "stylesheet")
    )
  )
}

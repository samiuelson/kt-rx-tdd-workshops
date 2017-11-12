package com.elpassion.kt.rx.tdd.workshops

import com.elpassion.kt.rx.tdd.workshops.SignUp.LoginValidation.LoginChangedEvent
import com.elpassion.kt.rx.tdd.workshops.SignUp.LoginValidation.State
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import junit.framework.Assert
import org.junit.Test

class SignUpLoginValidationReducerTest {

    private val apiSubject = PublishRelay.create<Boolean>()
    private val events = PublishRelay.create<Any>()
    private val reducer = LoginValidationReducer(object : SignUp.LoginValidation.Api {
        override fun call() = apiSubject
    })
    private val state = reducer.invoke(events).test()

    @Test
    fun shouldBeIdleAtTheBegging() {
        state.assertValue(State.IDLE)
    }

    @Test
    fun shouldBeInProgressWhenNotEmptyLoginArrives() {
        events.accept(LoginChangedEvent("login"))
        state.assertLastValue(State.LOADING)
    }

    @Test
    fun shouldBeIdleAfterErasingLogin() {
        events.accept(LoginChangedEvent("login"))
        events.accept(LoginChangedEvent(""))
        state.assertLastValue(State.IDLE)
    }

    @Test
    fun shouldReturnLoginOkWhenApiPasses() {
        events.accept(LoginChangedEvent("login"))
        apiSubject.accept(true)
        state.assertLastValue(State.LOGIN_OK)
    }

    class LoginValidationReducer(private val api: SignUp.LoginValidation.Api) : (Observable<Any>) -> Observable<State> {
        override fun invoke(events: Observable<Any>): Observable<State> {
            return events
                    .ofType(LoginChangedEvent::class.java)
                    .switchMap {
                        if (it.login.isEmpty()) {
                            Observable.just(State.IDLE)
                        } else {
                            api.call().map { State.LOGIN_OK }
                                    .startWith(State.LOADING)
                        }
                    }
                    .startWith(State.IDLE)
        }
    }

    fun <T> TestObserver<T>.assertLastValue(expected: T) {
        Assert.assertEquals(expected, values().last())
    }
}

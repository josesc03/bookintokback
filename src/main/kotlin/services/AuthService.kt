package services

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseToken

object AuthService {
    fun verifyToken(idToken: String): FirebaseToken {
        return FirebaseAuth.getInstance().verifyIdToken(idToken)
    }
}
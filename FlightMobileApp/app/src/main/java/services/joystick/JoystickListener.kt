package services.joystick

interface JoystickListener {
    fun onJoystickMoved(xPosition: Float, yPosition: Float, range: Float)
}
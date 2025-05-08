package exceptions

class UnauthorizedException(message: String) : Exception(message)
class NotFoundException(message: String) : Exception(message)
class ValidationException(message: String) : Exception(message)

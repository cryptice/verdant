package app.verdant.resource

import jakarta.ws.rs.ClientErrorException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ErrorMapperTest {
    @Test fun `optimistic planning conflicts remain 409 with a useful message`() {
        val response = ErrorMapper().toResponse(ClientErrorException("Preview changed. Preview again.", 409))
        assertEquals(409, response.status)
        assertEquals(ErrorMapper.ErrorResponse("Preview changed. Preview again.", 409), response.entity)
    }
}

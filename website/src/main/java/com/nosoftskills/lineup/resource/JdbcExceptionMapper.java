package com.nosoftskills.lineup.resource;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.hibernate.JDBCException;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.DataException;
import org.jboss.logging.Logger;

// App-wide fallback so unguarded persist()/flush() DB rejections never leak a raw stack trace.
@Provider
public class JdbcExceptionMapper implements ExceptionMapper<JDBCException> {

    private static final Logger LOG = Logger.getLogger(JdbcExceptionMapper.class);

    @CheckedTemplate
    static class Templates {
        static native TemplateInstance error(String message);
    }

    @Override
    public Response toResponse(JDBCException exception) {
        LOG.error("Database rejected the operation", exception);

        String message;
        if (exception instanceof DataException) {
            message = "Въведената стойност е твърде дълга или в невалиден формат.";
        } else if (exception instanceof ConstraintViolationException) {
            message = "Действието не може да бъде изпълнено -- нарушава ограничение на данните (например дублиран запис).";
        } else {
            message = "Възникна грешка при запис на данните.";
        }

        return Response.status(422)
                .type(MediaType.TEXT_HTML)
                .entity(Templates.error(message))
                .build();
    }
}

package iser.apiOrion.auth.filter;

import iser.apiOrion.auth.dto.TokenValidationResult;
import iser.apiOrion.auth.serviceImpl.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Enumeration;

@Service
public class CustomFilter extends OncePerRequestFilter {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Value("${valida.insertar-datos.requestURI.igual-noToken:total-lock}")
    private String insertarDatosRequestURI;

    @Value("${clave.valida.datos:total-lock}")
    private String claveValidaDatos;

    @Override
    public void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "POST, GET, PUT, DELETE");
        response.setHeader("Access-Control-Max-Age", "3600");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");

        // Manejar solicitudes OPTIONS (preflight)
        if (request.getMethod().equalsIgnoreCase("OPTIONS")) {
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        // Mostrar headers para depuración
        System.out.println("---------------------------------------------------------");
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            System.out.println(headerName + ": " + request.getHeader(headerName));
        }
        System.out.println("---------------------------------------------------------");

        System.out.println("Request Headers (Authorization): " + request.getHeader("Authorization"));
        System.out.println("Request Method: " + request.getMethod());
        System.out.println("Origin: " + request.getHeader("Origin"));
        System.out.println("URI: " + request.getRequestURI());
        System.out.println("¿No requiere token? " + this.jwtTokenProvider.requestURINoToken(request.getRequestURI()));

        boolean isNoTokenURI = this.jwtTokenProvider.requestURINoToken(request.getRequestURI());
        boolean isInsertarDatosValid = request.getRequestURI().equals(insertarDatosRequestURI)
                && claveValidaDatos.equals(request.getHeader("clave"));

        if (isNoTokenURI || isInsertarDatosValid) {
            System.out.println("No requiere token");
            chain.doFilter(request, response);
        } else {
            System.out.println("Requiere token");

            String token = this.jwtTokenProvider.extractToken(request);
            TokenValidationResult validationResult = this.jwtTokenProvider.resolveToken(token);

            if (validationResult.isValid()) {
                System.out.println("Token válido");
                response.addHeader("Authorization", jwtTokenProvider.createToken(
                        this.jwtTokenProvider.getSubject(token),
                        validationResult.getClaims().get("idUsuario").toString()
                ));
                response.addHeader("Access-Control-Expose-Headers", "Authorization");
                chain.doFilter(request, response);
            } else {
                System.out.println("Token inválido: " + validationResult.getMessage());
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, validationResult.getMessage());
            }
        }
    }
}

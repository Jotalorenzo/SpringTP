package com.example.trabajo_practico_spring;

import com.example.trabajo_practico_spring.dto.FacturaDetalleRequestDTO;
import com.example.trabajo_practico_spring.dto.FacturaRequestDTO;
import com.example.trabajo_practico_spring.service.AfipWsaaService;
import com.example.trabajo_practico_spring.service.AfipWsfeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class FacturaE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // Simulamos las llamadas externas de red a AFIP para que los tests corran
    // siempre
    @MockBean
    private AfipWsaaService afipWsaaService;

    @MockBean
    private AfipWsfeService afipWsfeService;

    // ---------------------------------------------------------------------------------
    // TEST 1: Crear Factura Borrador y verificar cálculo automático (201 Created)
    // ---------------------------------------------------------------------------------
    @Test
    @DisplayName("E2E 01: Debe crear una factura borrador calculando neto, IVA y total correctamente")
    void testCrearFacturaBorrador() throws Exception {
        FacturaRequestDTO request = FacturaRequestDTO.builder()
                .fechaEmision(LocalDate.now())
                .puntoVentaId(2L)
                .condicionIvaId(1L)
                .tipoMonedaId(1L)
                .listaPrecioId(1L)
                .letra("B")
                .clienteId(1L)
                .detalles(List.of(
                        FacturaDetalleRequestDTO.builder()
                                .articuloId(1L)
                                .cantidad(2)
                                .porcentajeBonificacion(10.0) // 10% descuento
                                .build()))
                .build();

        mockMvc.perform(post("/api/facturas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.estado", is("EMITIDA")))
                .andExpect(jsonPath("$.importeTotal", greaterThan(0.0)));
    }

    // ---------------------------------------------------------------------------------
    // TEST 2: Validar regla cronológica de fechas de emisión (409 Conflict)
    // ---------------------------------------------------------------------------------
    @Test
    @DisplayName("E2E 02: Debe rechazar factura con fecha de emisión anterior a la última registrada")
    void testValidarFechaCronologica() throws Exception {
        FacturaRequestDTO requestFechaInvalida = FacturaRequestDTO.builder()
                .fechaEmision(LocalDate.of(2020, 1, 1)) // Fecha muy vieja
                .puntoVentaId(1L)
                .condicionIvaId(1L)
                .tipoMonedaId(1L)
                .listaPrecioId(1L)
                .letra("B")
                .clienteId(1L)
                .detalles(List.of(
                        FacturaDetalleRequestDTO.builder()
                                .articuloId(1L)
                                .cantidad(1)
                                .porcentajeBonificacion(0.0)
                                .build()))
                .build();

        mockMvc.perform(post("/api/facturas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestFechaInvalida)))
                .andExpect(status().isConflict());
    }

    // ---------------------------------------------------------------------------------
    // TEST 3: Paginación y Filtros en el Servidor
    // ---------------------------------------------------------------------------------
    @Test
    @DisplayName("E2E 03: Debe consultar facturas de manera paginada y con metadatos de total")
    void testConsultarFacturasPaginadas() throws Exception {
        mockMvc.perform(get("/api/facturas")
                .param("page", "0")
                .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido").isArray())
                .andExpect(jsonPath("$.tamanio", is(5)))
                .andExpect(jsonPath("$.totalElementos", greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.totalPaginas", greaterThanOrEqualTo(1)));
    }

    // ---------------------------------------------------------------------------------
    // TEST 4: Explorer de Clientes con Límite de Resultados
    // ---------------------------------------------------------------------------------
    @Test
    @DisplayName("E2E 04: Explorer de clientes debe responder acotado sin saturar memoria")
    void testExplorerClientes() throws Exception {
        mockMvc.perform(get("/api/facturas/clientes/buscar")
                .param("q", "Tech"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // ---------------------------------------------------------------------------------
    // TEST 5: Explorer de Artículos por Lista de Precios
    // ---------------------------------------------------------------------------------
    @Test
    @DisplayName("E2E 05: Explorer de artículos debe traer precios según la lista indicada")
    void testExplorerArticulos() throws Exception {
        mockMvc.perform(get("/api/facturas/articulos/buscar")
                .param("listaPrecioId", "1")
                .param("q", "Teclado"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].precioVenta").exists());
    }

    // ---------------------------------------------------------------------------------
    // TEST 6: Autorización en ARCA / AFIP y Bloqueo Posterior de Modificación
    // ---------------------------------------------------------------------------------
    @Test
    @DisplayName("E2E 06: Debe autorizar en ARCA, obtener CAE y luego bloquear modificaciones")
    void testAutorizacionArcaYBloqueo() throws Exception {
        // 1. Simulación exitosa de WSAA y WSFE
        Mockito.when(afipWsaaService.obtenerTokenYSign()).thenReturn(new String[] { "TOKEN_MOCK", "SIGN_MOCK" });
        Mockito.when(afipWsfeService.solicitarCae(anyString(), anyString(), anyDouble(), anyString(), anyLong()))
                .thenReturn(new AfipWsfeService.ResultadoCae("86451067843704", LocalDate.now().plusDays(10), 99L));

        // 2. Crear una factura nueva
        FacturaRequestDTO request = FacturaRequestDTO.builder()
                .fechaEmision(LocalDate.now().plusDays(1))
                .puntoVentaId(2L)
                .condicionIvaId(1L)
                .tipoMonedaId(1L)
                .listaPrecioId(1L)
                .letra("B")
                .clienteId(1L)
                .detalles(List.of(
                        FacturaDetalleRequestDTO.builder().articuloId(1L).cantidad(1).porcentajeBonificacion(0.0)
                                .build()))
                .build();

        String responseJson = mockMvc.perform(post("/api/facturas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long idCreada = objectMapper.readTree(responseJson).get("id").asLong();

        // 3. Informar a ARCA
        mockMvc.perform(put("/api/facturas/" + idCreada + "/informar-arca"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado", is("AUTORIZADA")))
                .andExpect(jsonPath("$.cae", is("86451067843704")));

        // 4. Intentar modificarla (debe arrojar 409 Conflict por regla de negocio)
        mockMvc.perform(put("/api/facturas/" + idCreada)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());

        // 5. Intentar eliminarla físicamente (debe arrojar 409 Conflict)
        mockMvc.perform(delete("/api/facturas/" + idCreada))
                .andExpect(status().isConflict());
    }

    // ---------------------------------------------------------------------------------
    // TEST 7: Factura X no permite ser informada a ARCA
    // ---------------------------------------------------------------------------------
    @Test
    @DisplayName("E2E 07: Factura X debe ser rechazada si se intenta informar a ARCA")
    void testFacturaXNoFiscal() throws Exception {
        FacturaRequestDTO requestX = FacturaRequestDTO.builder()
                .fechaEmision(LocalDate.now().plusDays(2))
                .puntoVentaId(2L)
                .condicionIvaId(1L)
                .tipoMonedaId(1L)
                .listaPrecioId(1L)
                .letra("X") // Presupuesto
                .clienteId(1L)
                .detalles(List.of(
                        FacturaDetalleRequestDTO.builder().articuloId(1L).cantidad(1).porcentajeBonificacion(0.0)
                                .build()))
                .build();

        String responseJson = mockMvc.perform(post("/api/facturas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestX)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long idCreada = objectMapper.readTree(responseJson).get("id").asLong();

        // Intentar enviar a ARCA debe dar 409
        mockMvc.perform(put("/api/facturas/" + idCreada + "/informar-arca"))
                .andExpect(status().isConflict());
    }

    // ---------------------------------------------------------------------------------
    // TEST 8: Generación y Descarga de PDF Legal
    // ---------------------------------------------------------------------------------
    @Test
    @DisplayName("E2E 08: Debe generar el PDF de una factura con encabezados correctos")
    void testDescargarPdf() throws Exception {
        mockMvc.perform(get("/api/facturas/1/pdf"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_PDF_VALUE))
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }
}
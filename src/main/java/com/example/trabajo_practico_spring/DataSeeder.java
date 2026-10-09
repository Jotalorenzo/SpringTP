package com.example.trabajo_practico_spring;

import com.example.trabajo_practico_spring.model.*;
import com.example.trabajo_practico_spring.repository.ArticuloRepository;
import com.example.trabajo_practico_spring.repository.ClienteRepository;
import com.example.trabajo_practico_spring.repository.CondicionIvaRepository;
import com.example.trabajo_practico_spring.repository.FacturaVentaRepository;
import com.example.trabajo_practico_spring.repository.ListaPrecioArticuloRepository;
import com.example.trabajo_practico_spring.repository.ListaPrecioRepository;
import com.example.trabajo_practico_spring.repository.PuntoVentaRepository;
import com.example.trabajo_practico_spring.repository.TipoMonedaRepository;
import com.example.trabajo_practico_spring.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Carga un juego de datos inicial para poder probar el reporte.
 * No corre si la base ya tiene facturas.
 */
@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner seed(UsuarioRepository usuarios,
                           CondicionIvaRepository condiciones,
                           TipoMonedaRepository monedas,
                           PuntoVentaRepository puntosVenta,
                           ClienteRepository clientes,
                           ArticuloRepository articulos,
                           ListaPrecioRepository listasPrecio,
                           ListaPrecioArticuloRepository preciosArticulo,
                           FacturaVentaRepository facturas) {
        return args -> cargar(usuarios, condiciones, monedas, puntosVenta, clientes,
                articulos, listasPrecio, preciosArticulo, facturas);
    }

    @Transactional
    void cargar(UsuarioRepository usuarios,
                CondicionIvaRepository condiciones,
                TipoMonedaRepository monedas,
                PuntoVentaRepository puntosVenta,
                ClienteRepository clientes,
                ArticuloRepository articulos,
                ListaPrecioRepository listasPrecio,
                ListaPrecioArticuloRepository preciosArticulo,
                FacturaVentaRepository facturas) {

        if (facturas.count() > 0) {
            return;
        }

        Usuario admin = new Usuario();
        admin.setUsuario("admin");
        admin.setClave("admin123");
        admin.setNombre("Juan");
        admin.setApellido("Perez");
        admin = usuarios.save(admin);

        CondicionIva responsable = new CondicionIva();
        responsable.setCodigoAfip(1);
        responsable.setDenominacion("IVA Responsable Inscripto");
        completarAuditoria(responsable, admin);
        responsable = condiciones.save(responsable);

        CondicionIva consumidor = new CondicionIva();
        consumidor.setCodigoAfip(5);
        consumidor.setDenominacion("Consumidor Final");
        completarAuditoria(consumidor, admin);
        consumidor = condiciones.save(consumidor);

        TipoMoneda pesos = new TipoMoneda();
        pesos.setCodigoAfip("PES");
        pesos.setDenominacion("Pesos Argentinos");
        pesos.setSimbolo("$");
        completarAuditoria(pesos, admin);
        pesos = monedas.save(pesos);

        PuntoVenta central = new PuntoVenta();
        central.setNumero(1);
        central.setDescripcion("Sucursal Central");
        central.setTipoEmision("CAE");
        central.setDomicilioComercial("Av. Corrientes 800");
        completarAuditoria(central, admin);
        central = puntosVenta.save(central);

        PuntoVenta norte = new PuntoVenta();
        norte.setNumero(2);
        norte.setDescripcion("Sucursal Norte");
        norte.setTipoEmision("CAE");
        norte.setDomicilioComercial("Av. Cabildo 1500");
        completarAuditoria(norte, admin);
        norte = puntosVenta.save(norte);

        Cliente empresa = new Cliente();
        empresa.setCuitCuil("30-71234567-9");
        empresa.setDenominacion("Empresa Tech S.A.");
        empresa.setContacto(nuevoContacto("contacto@empresatech.com", "011-4321-0001", "11-5555-1111"));
        empresa.setDomicilio(nuevoDomicilio("Av. Santa Fe", "1200"));
        completarAuditoria(empresa, admin);
        empresa = clientes.save(empresa);

        Cliente minorista = new Cliente();
        minorista.setCuitCuil("20-98765432-1");
        minorista.setDenominacion("Distribuidora del Sur S.R.L.");
        minorista.setContacto(nuevoContacto("ventas@distribuidorasur.com", "011-5555-1234", "11-6666-2222"));
        minorista.setDomicilio(nuevoDomicilio("Calle 45", "678"));
        completarAuditoria(minorista, admin);
        minorista = clientes.save(minorista);

        Cliente anonimo = new Cliente();
        anonimo.setCuitCuil("20-11111111-1");
        anonimo.setDenominacion("Consumidor Final");
        anonimo.setContacto(nuevoContacto("consumidor@ejemplo.com", "011-0000-0000", null));
        anonimo.setDomicilio(nuevoDomicilio("Sin domicilio", "-"));
        completarAuditoria(anonimo, admin);
        anonimo = clientes.save(anonimo);

        Articulo notebook = new Articulo();
        notebook.setCodigo("ART-001");
        notebook.setDenominacion("Notebook 14 pulgadas");
        completarAuditoria(notebook, admin);
        notebook = articulos.save(notebook);

        Articulo teclado = new Articulo();
        teclado.setCodigo("ART-002");
        teclado.setDenominacion("Teclado mecanico");
        completarAuditoria(teclado, admin);
        teclado = articulos.save(teclado);

        Articulo mouse = new Articulo();
        mouse.setCodigo("ART-003");
        mouse.setDenominacion("Mouse inalambrico");
        completarAuditoria(mouse, admin);
        mouse = articulos.save(mouse);

        Articulo monitor = new Articulo();
        monitor.setCodigo("ART-004");
        monitor.setDenominacion("Monitor 24 pulgadas");
        completarAuditoria(monitor, admin);
        monitor = articulos.save(monitor);

        ListaPrecio listaMayorista = new ListaPrecio();
        listaMayorista.setCodigo("LP-001");
        listaMayorista.setDenominacion("Lista Mayorista");
        completarAuditoria(listaMayorista, admin);
        listaMayorista = listasPrecio.save(listaMayorista);

        ListaPrecioArticulo precioNotebook = precioLista(listaMayorista, notebook, 7500.0, admin, preciosArticulo);
        ListaPrecioArticulo precioTeclado = precioLista(listaMayorista, teclado, 2200.0, admin, preciosArticulo);
        ListaPrecioArticulo precioMouse = precioLista(listaMayorista, mouse, 1500.0, admin, preciosArticulo);
        ListaPrecioArticulo precioMonitor = precioLista(listaMayorista, monitor, 8000.0, admin, preciosArticulo);

        // Facturas. La 1001 no tiene cliente, para ejercitar el COALESCE a "Consumidor Final".
        facturas.save(factura(1001L, LocalDate.of(2026, 3, 15), central, null, consumidor, pesos,
                1500.0, "EMITIDA", admin,
                List.of(detalle(precioNotebook, 2, admin))));

        facturas.save(factura(1002L, LocalDate.of(2026, 3, 20), central, empresa, responsable, pesos,
                85000.5, "EMITIDA", admin,
                List.of(detalle(precioNotebook, 10, admin),
                        detalle(precioTeclado, 5, admin),
                        detalle(precioMouse, 8, admin))));

        facturas.save(factura(1003L, LocalDate.of(2026, 2, 10), norte, minorista, consumidor, pesos,
                32000.0, "ANULADA", admin,
                List.of(detalle(precioMonitor, 4, admin))));

        facturas.save(factura(1004L, LocalDate.of(2026, 4, 5), norte, anonimo, responsable, pesos,
                45000.0, "EMITIDA", admin,
                List.of(detalle(precioMonitor, 3, admin))));
    }

    private ListaPrecioArticulo precioLista(ListaPrecio lista, Articulo articulo,
                                            double precio, Usuario admin,
                                            ListaPrecioArticuloRepository preciosArticulo) {
        ListaPrecioArticulo lpa = new ListaPrecioArticulo();
        lpa.setListaPrecio(lista);
        lpa.setArticulo(articulo);
        lpa.setPrecioVenta(precio);
        completarAuditoria(lpa, admin);
        return preciosArticulo.save(lpa);
    }

    private Contacto nuevoContacto(String email, String telefono, String celular) {
        Contacto contacto = new Contacto();
        contacto.setEmail(email);
        contacto.setTelefono(telefono);
        contacto.setCelular(celular);
        return contacto;
    }

    private Domicilio nuevoDomicilio(String calle, String numero) {
        Domicilio domicilio = new Domicilio();
        domicilio.setNombreCalle(calle);
        domicilio.setNumeroCalle(numero);
        return domicilio;
    }

    private FacturaVenta factura(Long numero, LocalDate fecha, PuntoVenta puntoVenta, Cliente cliente,
                                 CondicionIva condicion, TipoMoneda moneda, double total,
                                 String estado, Usuario admin, List<FacturaVentaDetalle> detalles) {
        FacturaVenta f = new FacturaVenta();
        f.setNumero(numero);
        f.setFechaEmision(fecha);
        f.setPuntoVenta(puntoVenta);
        f.setCliente(cliente);
        f.setCondicionIva(condicion);
        f.setTipoMoneda(moneda);
        f.setImporteTotal(total);
        boolean emitida = "EMITIDA".equals(estado);
        f.setImporteCobrado(emitida ? total : 0.0);
        f.setImporteSaldo(emitida ? 0.0 : total);
        f.setEstado(estado);
        f.setCae(String.format("CAE%011d", numero));
        f.setCaeFechaVencimiento(fecha.plusDays(10));
        f.setResultadoAfip("APROBADO");
        completarAuditoria(f, admin);
        detalles.forEach(f::agregarDetalle);
        return f;
    }

    private FacturaVentaDetalle detalle(ListaPrecioArticulo precioArticulo, double cantidad, Usuario admin) {
        double precio = precioArticulo.getPrecioVenta();
        FacturaVentaDetalle d = new FacturaVentaDetalle();
        d.setListaPrecioArticulo(precioArticulo);
        d.setDescripcion(precioArticulo.getArticulo().getDenominacion());
        d.setCantidad(cantidad);
        d.setPrecioUnitario(precio);
        d.setPorcentajeBonificacion(0.0);
        d.setImporteNeto(cantidad * precio);
        d.setImporteIva(cantidad * precio * 0.21);
        d.setImporteSubtotal(cantidad * precio);
        return d;
    }

    private void completarAuditoria(AuditoriaApp entidad, Usuario admin) {
        LocalDateTime ahora = LocalDateTime.now();
        entidad.setFechaAlta(ahora);
        entidad.setFechaModificacion(ahora);
        entidad.setUsuarioCarga(admin);
        entidad.setUsuarioModificacion(admin);
    }
}

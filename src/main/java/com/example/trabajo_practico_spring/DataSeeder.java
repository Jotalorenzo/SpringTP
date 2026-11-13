package com.example.trabajo_practico_spring;

import com.example.trabajo_practico_spring.model.*;
import com.example.trabajo_practico_spring.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

        // Si ya existen 50 artículos en la base, no duplicamos
        if (articulos.count() >= 50 && clientes.count() >= 50) {
            return;
        }

        // ------------------------------------------------------------- USUARIO AUDITORÍA
        Usuario admin = usuarios.findByUsuario("admin").orElseGet(() -> {
            Usuario u = new Usuario();
            u.setUsuario("admin");
            u.setClave("admin123");
            u.setNombre("Juan");
            u.setApellido("Perez");
            return usuarios.save(u);
        });

        // ------------------------------------------------------------- CONDICIONES IVA
        CondicionIva responsable = condiciones.findById(1L).orElseGet(() -> {
            CondicionIva ci = new CondicionIva();
            ci.setCodigoAfip(1);
            ci.setDenominacion("IVA Responsable Inscripto");
            completarAuditoria(ci, admin);
            return condiciones.save(ci);
        });

        CondicionIva consumidor = condiciones.findById(2L).orElseGet(() -> {
            CondicionIva ci = new CondicionIva();
            ci.setCodigoAfip(5);
            ci.setDenominacion("Consumidor Final");
            completarAuditoria(ci, admin);
            return condiciones.save(ci);
        });

        // ------------------------------------------------------------- MONEDAS
        TipoMoneda pesos = monedas.findAll().stream().findFirst().orElseGet(() -> {
            TipoMoneda m = new TipoMoneda();
            m.setCodigoAfip("PES");
            m.setDenominacion("Pesos Argentinos");
            m.setSimbolo("$");
            completarAuditoria(m, admin);
            return monedas.save(m);
        });

        // ------------------------------------------------------------- PUNTOS DE VENTA
        PuntoVenta central = puntosVenta.findById(1L).orElseGet(() -> {
            PuntoVenta pv = new PuntoVenta();
            pv.setNumero(1);
            pv.setDescripcion("Sucursal Central");
            pv.setTipoEmision("CAE");
            pv.setDomicilioComercial("Av. Corrientes 800, CABA");
            completarAuditoria(pv, admin);
            return puntosVenta.save(pv);
        });

        PuntoVenta norte = puntosVenta.findById(2L).orElseGet(() -> {
            PuntoVenta pv = new PuntoVenta();
            pv.setNumero(2);
            pv.setDescripcion("Sucursal Norte");
            pv.setTipoEmision("CAE");
            pv.setDomicilioComercial("Av. Cabildo 1500, CABA");
            completarAuditoria(pv, admin);
            return puntosVenta.save(pv);
        });

        // ------------------------------------------------------------- LISTAS DE PRECIOS
        ListaPrecio listaMayorista = listasPrecio.findById(1L).orElseGet(() -> {
            ListaPrecio lp = new ListaPrecio();
            lp.setCodigo("LP-001");
            lp.setDenominacion("Lista Mayorista");
            completarAuditoria(lp, admin);
            return listasPrecio.save(lp);
        });

        ListaPrecio listaMinorista = listasPrecio.findById(2L).orElseGet(() -> {
            ListaPrecio lp = new ListaPrecio();
            lp.setCodigo("LP-002");
            lp.setDenominacion("Lista Minorista");
            completarAuditoria(lp, admin);
            return listasPrecio.save(lp);
        });

        // ------------------------------------------------------------- 50 CLIENTES
        List<Cliente> clientesList = new ArrayList<>();
        String[] rubrosEmpresas = {"Tecnología", "Distribuidora", "Logística", "Comercial", "Soluciones", "Industrias", "Servicios", "Insumos", "Importadora", "Electrónica"};
        String[] nombresPersonas = {"Juan Pérez", "María González", "Carlos Rodríguez", "Ana Fernández", "Lucas Gómez", "Laura Díaz", "Martín Romero", "Sofía Álvarez", "Diego Torres", "Valentina Ruiz"};

        for (int i = 1; i <= 50; i++) {
            Cliente c = new Cliente();
            String cuit;
            String denominacion;
            if (i <= 25) {
                // Empresas (CUIT 30-)
                cuit = String.format("30-%08d-%d", 70000000 + i, (i % 9) + 1);
                denominacion = rubrosEmpresas[(i - 1) % rubrosEmpresas.length] + " " + (i % 2 == 0 ? "S.A." : "S.R.L.") + " #" + i;
            } else {
                // Personas (CUIT 20- / 27-)
                cuit = String.format("20-%08d-%d", 30000000 + i, (i % 9) + 1);
                denominacion = nombresPersonas[(i - 26) % nombresPersonas.length] + " (" + i + ")";
            }

            c.setCuitCuil(cuit);
            c.setDenominacion(denominacion);
            c.setContacto(nuevoContacto("cliente" + i + "@empresa.com", "011-4500-" + String.format("%04d", i), "11-6000-" + String.format("%04d", i)));
            c.setDomicilio(nuevoDomicilio("Calle Falsa", String.valueOf(100 + i * 10)));
            completarAuditoria(c, admin);
            try {
                clientesList.add(clientes.save(c));
            } catch (Exception ignored) {}
        }

        // ------------------------------------------------------------- 50 ARTÍCULOS
        String[] catalogoNombres = {
            "Notebook Lenovo 14 ThinkPad", "Notebook Dell Vostro 15", "Notebook HP Pavilion 15", "Notebook ASUS ZenBook 14", "PC Gamer Intel Core i7",
            "Monitor Samsung 24 IPS FHD", "Monitor LG 27 Gamer 144Hz", "Monitor Dell 32 Curvo 4K", "Monitor ViewSonic 19 Oficina", "Monitor ASUS TUF 24",
            "Teclado Mecanico RGB Redragon", "Teclado Inalambrico Logitech K380", "Teclado Gamer Corsair K55", "Mouse Optico USB Logitech G203", "Mouse Inalambrico Logitech MX Master",
            "Mouse Gamer Razer DeathAdder", "Auriculares HyperX Cloud II", "Auriculares Inalambricos Sony WH", "Auriculares Redragon Zeus", "Microfono USB Blue Yeti",
            "Webcam Logitech C920 Pro HD", "Disco SSD Kingston 480GB SATA", "Disco SSD NVMe Samsung 1TB", "Disco SSD Crucial 2TB PCIe", "Disco Rigido Externo WD 2TB",
            "Memoria RAM Kingston 8GB DDR4", "Memoria RAM Corsair 16GB DDR4", "Memoria RAM Fury 32GB DDR5", "Placa de Video RTX 4060 8GB", "Placa de Video RX 7600 8GB",
            "Fuente Corsair 650W 80 Plus", "Fuente EVGA 750W Gold", "Gabinete Gamer Vidrio Templado", "Cooler CPU CoolerMaster Hyper 212", "Watercooling Corsair H100i",
            "Motherboard ASUS B550-Plus", "Motherboard Gigabyte Z790", "Procesador AMD Ryzen 5 5600X", "Procesador Intel Core i5 13400", "Procesador AMD Ryzen 7 7700X",
            "Router Wi-Fi 6 TP-Link Archer", "Switch Gigabit TP-Link 8 Puertos", "Placa de Red Wi-Fi USB Dual Band", "Cable HDMI 2.1 2 Metros 8K", "Cable DisplayPort 1.4 2 Metros",
            "Hub USB Tipo C 7 en 1", "Impresora Multifuncion HP Laser", "Impresora Termica Comandas 80mm", "Lector Codigo de Barras Laser", "Estabilizador de Tension 1000VA"
        };

        double[] preciosBase = {
            85000, 92000, 88000, 110000, 150000,
            24000, 35000, 58000, 16000, 29000,
            5500, 4200, 7800, 3200, 14500,
            4800, 12500, 22000, 9800, 18500,
            11000, 4500, 13500, 21000, 8900,
            3800, 6900, 14200, 65000, 54000,
            12000, 16500, 8500, 4500, 19500,
            18500, 28000, 24000, 26500, 39000,
            7500, 4200, 2100, 1200, 1500,
            3800, 32000, 18500, 4900, 5600
        };

        List<ListaPrecioArticulo> preciosMayoristas = new ArrayList<>();
        List<ListaPrecioArticulo> preciosMinoristas = new ArrayList<>();

        for (int i = 0; i < catalogoNombres.length; i++) {
            Articulo a = new Articulo();
            a.setCodigo(String.format("ART-%03d", i + 1));
            a.setDenominacion(catalogoNombres[i]);
            completarAuditoria(a, admin);
            try {
                a = articulos.save(a);
            } catch (Exception ignored) {
                continue;
            }

            double base = preciosBase[i];

            // 1. Precio Mayorista
            ListaPrecioArticulo lpaMay = precioLista(listaMayorista, a, base, admin, preciosArticulo);
            preciosMayoristas.add(lpaMay);

            // 2. Precio Minorista (+30%)
            ListaPrecioArticulo lpaMin = precioLista(listaMinorista, a, Math.round(base * 1.30 * 100.0) / 100.0, admin, preciosArticulo);
            preciosMinoristas.add(lpaMin);
        }

        // ------------------------------------------------------------- FACTURAS DE EJEMPLO
        if (facturas.count() == 0 && !preciosMayoristas.isEmpty()) {
            facturas.save(factura(1001L, LocalDate.of(2026, 3, 15), central, null, consumidor, pesos, listaMayorista, "B",
                    1500.0, "EMITIDA", admin,
                    List.of(detalle(preciosMayoristas.get(0), 2, admin))));

            Cliente emp = clientesList.isEmpty() ? null : clientesList.get(0);
            facturas.save(factura(1002L, LocalDate.of(2026, 3, 20), central, emp, responsable, pesos, listaMayorista, "A",
                    85000.5, "EMITIDA", admin,
                    List.of(detalle(preciosMayoristas.get(0), 1, admin),
                            detalle(preciosMayoristas.get(1), 2, admin))));

            Cliente min = clientesList.size() > 1 ? clientesList.get(1) : null;
            facturas.save(factura(1003L, LocalDate.of(2026, 2, 10), norte, min, consumidor, pesos, listaMinorista, "B",
                    32000.0, "ANULADA", admin,
                    List.of(detalle(preciosMinoristas.get(2), 4, admin))));

            facturas.save(factura(1004L, LocalDate.of(2026, 4, 5), norte, min, responsable, pesos, listaMayorista, "X",
                    45000.0, "EMITIDA", admin,
                    List.of(detalle(preciosMayoristas.get(3), 3, admin))));
        }
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
                                 CondicionIva condicion, TipoMoneda moneda, ListaPrecio lista, String letra,
                                 double total, String estado, Usuario admin, List<FacturaVentaDetalle> detalles) {
        FacturaVenta f = new FacturaVenta();
        f.setNumero(numero);
        f.setFechaEmision(fecha);
        f.setPuntoVenta(puntoVenta);
        f.setCliente(cliente);
        f.setCondicionIva(condicion);
        f.setTipoMoneda(moneda);
        f.setListaPrecio(lista);
        f.setLetra(letra);
        f.setImporteTotal(total);
        boolean emitida = "EMITIDA".equals(estado);
        f.setImporteCobrado(emitida ? total : 0.0);
        f.setImporteSaldo(emitida ? 0.0 : total);
        f.setEstado(estado);
        if (!"X".equalsIgnoreCase(letra)) {
            f.setCae(String.format("CAE%011d", numero));
            f.setCaeFechaVencimiento(fecha.plusDays(10));
            f.setResultadoAfip("APROBADO");
        }
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
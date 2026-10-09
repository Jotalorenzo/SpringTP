package com.example.trabajo_practico_spring.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "factura_venta")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true, exclude = {"detalles"})
@ToString(exclude = {"detalles", "observaciones", "motivoRechazo"})
public class FacturaVenta extends AuditoriaApp {

    @Column(name = "numero", nullable = false)
    private Long numero;

    @Column(name = "fecha_emision", nullable = false)
    private LocalDate fechaEmision;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "punto_venta_id", nullable = false)
    private PuntoVenta puntoVenta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_moneda_id")
    private TipoMoneda tipoMoneda;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "condicion_iva_id")
    private CondicionIva condicionIva;

    @Column(name = "importe_cobrado", nullable = false)
    private double importeCobrado;

    @Column(name = "importe_saldo", nullable = false)
    private double importeSaldo;

    @Column(name = "importe_total", nullable = false)
    private double importeTotal;

    @Column(name = "cae", length = 20)
    private String cae;

    @Column(name = "cae_fecha_vencimiento")
    private LocalDate caeFechaVencimiento;

    @Column(name = "resultado_afip", length = 120)
    private String resultadoAfip;

    @Column(name = "motivo_rechazo", length = 255)
    private String motivoRechazo;

    @Column(name = "estado", nullable = false, length = 30)
    private String estado;

    @Column(name = "fecha_anulacion")
    private LocalDate fechaAnulacion;

    @Column(name = "observaciones", length = 255)
    private String observaciones;

    @Builder.Default
    @OneToMany(mappedBy = "factura", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FacturaVentaDetalle> detalles = new ArrayList<>();

    public void agregarDetalle(FacturaVentaDetalle detalle) {
        detalle.setFactura(this);
        this.detalles.add(detalle);
    }

    public void quitarDetalle(FacturaVentaDetalle detalle) {
        this.detalles.remove(detalle);
        detalle.setFactura(null);
    }
}

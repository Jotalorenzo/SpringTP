package com.example.trabajo_practico_spring.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "factura_venta_detalle")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true, exclude = {"factura", "listaPrecioArticulo"})
@ToString(exclude = {"factura", "listaPrecioArticulo", "descripcion"})
public class FacturaVentaDetalle extends EntityId {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "factura_venta_id", nullable = false)
    private FacturaVenta factura;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lista_precio_articulo_id", nullable = false)
    private ListaPrecioArticulo listaPrecioArticulo;

    @Column(name = "descripcion", length = 200)
    private String descripcion;

    @Column(name = "cantidad", nullable = false)
    private double cantidad;

    @Column(name = "precio_unitario", nullable = false)
    private double precioUnitario;

    @Column(name = "porcentaje_bonificacion", nullable = false)
    private double porcentajeBonificacion;

    @Column(name = "importe_neto", nullable = false)
    private double importeNeto;

    @Column(name = "importe_iva", nullable = false)
    private double importeIva;

    @Column(name = "importe_subtotal", nullable = false)
    private double importeSubtotal;
}

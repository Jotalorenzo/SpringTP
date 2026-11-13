import { facturaService } from "./facturaService.js";

let detallesActuales = [];
let facturaSeleccionadaId = null;
let facturaEditable = true;
let paginaActual = 0;
let listaClientes = [];
let listaArticulos = [];

// 1. Cargador modular de vistas HTML
async function cargarVistasHTML() {
  const vistas = [
    { id: "vista-historial", url: "./views/historial.html" },
    { id: "vista-formulario", url: "./views/formulario-factura.html" },
    { id: "vista-acciones", url: "./views/acciones.html" },
    { id: "vista-modales", url: "./views/modales.html" },
  ];

  await Promise.all(
    vistas.map(async (v) => {
      const resp = await fetch(v.url);
      const html = await resp.text();
      document.getElementById(v.id).innerHTML = html;
    })
  );
}

document.addEventListener("DOMContentLoaded", async () => {
  await cargarVistasHTML();
  await inicializarFormulario();
  configurarEventos();
  configurarModalesExplorer();
  await cargarGrillaFacturas();
});

async function inicializarFormulario() {
  try {
    const [clientes, puntosVenta, monedas, articulos] = await Promise.all([
      facturaService.obtenerClientes(),
      facturaService.obtenerPuntosVenta(),
      facturaService.obtenerTiposMoneda(),
      facturaService.obtenerArticulos(),
    ]);
    listaClientes = clientes;
    listaArticulos = articulos;

    llenarSelect("puntoVentaSelect", puntosVenta, "id", "descripcion", true);
    llenarSelect("monedaSelect", monedas, "id", "denominacion", true);
  } catch (error) {
    console.error("Error al cargar paramétricas:", error);
  }
}

function llenarSelect(selectId, items, valueProp, textProp, seleccionarPrimero = false) {
  const select = document.getElementById(selectId);
  if (!select) return;
  select.innerHTML = '<option value="">Seleccione una opción...</option>';
  items.forEach((item, index) => {
    const option = document.createElement("option");
    option.value = String(item[valueProp]);
    option.textContent = item[textProp] || `ID: ${item[valueProp]}`;
    if (seleccionarPrimero && index === 0) {
      option.selected = true;
    }
    select.appendChild(option);
  });
}

async function calcularSiguienteNumeroFactura() {
  try {
    const pagina = await facturaService.buscarFacturasPaginadas({
      informadaArca: true,
      page: 0,
      size: 1000,
    });
    const facturasConCae = (pagina.contenido || []).filter((f) => f.cae);

    if (facturasConCae.length === 0) return 1;

    facturasConCae.sort((a, b) => b.id - a.id);

    const ultimaInformada = facturasConCae[0];
    const ultimoNumero = parseInt(
      ultimaInformada.numeroFactura ?? ultimaInformada.numero ?? 0,
      10
    );

    return !isNaN(ultimoNumero) ? ultimoNumero + 1 : 1;
  } catch (error) {
    console.error("Error al calcular el siguiente número de factura de ARCA:", error);
    return 1;
  }
}

function configurarEventos() {
  // Abrir panel de Nueva Factura y autogenerar el Número (Última + 1)
  document.getElementById("btnAbrirNuevaFactura")?.addEventListener("click", async () => {
    await resetearFormulario();
    document.getElementById("tituloFormulario").textContent = "Nueva Factura de Venta";
    document.getElementById("contenedor-panel-factura").style.display = "block";
    document.getElementById("contenedor-panel-factura").scrollIntoView({ behavior: "smooth" });
  });

  document.getElementById("btnCerrarFormulario")?.addEventListener("click", () => {
    document.getElementById("contenedor-panel-factura").style.display = "none";
  });

  // Recalcular precios si cambia la Lista de Precios
  document.getElementById("listaPrecioSelect")?.addEventListener("change", () => {
    if (facturaEditable) {
      recalcularPreciosPorLista();
    }
  });

  document.getElementById("btnAgregarLinea")?.addEventListener("click", agregarLinea);
  document.getElementById("btnGuardarFactura")?.addEventListener("click", enviarFactura);
  document.getElementById("btnActualizarFactura")?.addEventListener("click", actualizarFacturaExistente);

  document.getElementById("btnFiltrar")?.addEventListener("click", () => {
    paginaActual = 0;
    cargarGrillaFacturas();
  });
  document.getElementById("btnLimpiarFiltros")?.addEventListener("click", limpiarFiltros);
  document.getElementById("btnPaginaAnterior")?.addEventListener("click", () => {
    if (paginaActual > 0) {
      paginaActual--;
      cargarGrillaFacturas();
    }
  });
  document.getElementById("btnPaginaSiguiente")?.addEventListener("click", () => {
    paginaActual++;
    cargarGrillaFacturas();
  });

  document.getElementById("btnInformarArca")?.addEventListener("click", informarArca);
  document.getElementById("btnAnularFactura")?.addEventListener("click", anularFactura);
  document.getElementById("btnEliminarFactura")?.addEventListener("click", eliminarFactura);
  document.getElementById("btnImprimirPdf")?.addEventListener("click", imprimirPdf);
}

// -------------------------------------------------------------
// CONTROL DE MODO EDICIÓN vs SOLO LECTURA (ARCA / ANULADA)
// -------------------------------------------------------------
function configurarModoEdicion(editable) {
  facturaEditable = editable;

  document.getElementById("fechaEmisionInput").disabled = !editable;
  document.getElementById("tipoComprobanteSelect").disabled = !editable;
  document.getElementById("listaPrecioSelect").disabled = !editable;
  document.getElementById("puntoVentaSelect").disabled = !editable;
  document.getElementById("monedaSelect").disabled = !editable;
  document.getElementById("btnAbrirModalCliente").disabled = !editable;

  const barraArt = document.getElementById("barraAgregarArticulo");
  if (barraArt) barraArt.style.display = editable ? "flex" : "none";

  const banner = document.getElementById("bannerSoloLectura");
  if (banner) banner.style.display = editable ? "none" : "block";
}

// -------------------------------------------------------------
// LÓGICA DE INTERFACES EXPLORER (MODALES)
// -------------------------------------------------------------
function configurarModalesExplorer() {
  const modalCli = document.getElementById("modalCliente");
  const inputNombre = document.getElementById("filtroModalClienteNombre");
  const inputCuit = document.getElementById("filtroModalClienteCuit");

  const filtrarClientes = () => {
    renderizarTablaModalClientes(inputNombre?.value || "", inputCuit?.value || "");
  };

  document.getElementById("btnAbrirModalCliente")?.addEventListener("click", () => {
    if (!facturaEditable) return;
    if (inputNombre) inputNombre.value = "";
    if (inputCuit) inputCuit.value = "";
    renderizarTablaModalClientes("", "");
    modalCli.style.display = "flex";
  });
  document.getElementById("btnCerrarModalCliente")?.addEventListener("click", () => {
    modalCli.style.display = "none";
  });
  inputNombre?.addEventListener("input", filtrarClientes);
  inputCuit?.addEventListener("input", filtrarClientes);

  const modalArt = document.getElementById("modalArticulo");
  document.getElementById("btnAbrirModalArticulo")?.addEventListener("click", () => {
    if (!facturaEditable) return;
    renderizarTablaModalArticulos("");
    modalArt.style.display = "flex";
  });
  document.getElementById("btnCerrarModalArticulo")?.addEventListener("click", () => {
    modalArt.style.display = "none";
  });
  document.getElementById("filtroModalArticulo")?.addEventListener("input", (e) => {
    renderizarTablaModalArticulos(e.target.value);
  });
}

function renderizarTablaModalClientes(filtroNombre = "", filtroCuit = "") {
  const tbody = document.querySelector("#tablaModalClientes tbody");
  tbody.innerHTML = "";
  const termNombre = filtroNombre.toLowerCase().trim();
  const termCuit = filtroCuit.toLowerCase().trim();

  const filtrados = listaClientes.filter((c) => {
    const coincideNombre = !termNombre || (c.denominacion && c.denominacion.toLowerCase().includes(termNombre));
    const coincideCuit = !termCuit || (c.cuitCuil && String(c.cuitCuil).toLowerCase().includes(termCuit));
    return coincideNombre && coincideCuit;
  });

  filtrados.forEach((c) => {
    const tr = document.createElement("tr");
    tr.innerHTML = `
      <td>${c.id}</td>
      <td>${c.denominacion || ""}</td>
      <td>${c.cuitCuil || "Consumidor Final"}</td>
      <td><button type="button" class="btn-primary" style="padding: 4px 8px;" onclick="seleccionarClienteModal(${c.id})">Elegir</button></td>
    `;
    tbody.appendChild(tr);
  });
}

window.seleccionarClienteModal = (id) => {
  const cliente = listaClientes.find((c) => c.id === id);
  document.getElementById("clienteIdHidden").value = String(id);
  document.getElementById("clienteNombreInput").value = cliente
    ? `${cliente.denominacion} (${cliente.cuitCuil || "CF"})`
    : `ID: ${id}`;
  document.getElementById("modalCliente").style.display = "none";
};

function obtenerPreciosArticulo(articulo) {
  const base = Number(articulo.precioVenta ?? articulo.precio ?? 0);
  const minorista = Number(articulo.precioMinorista ?? base);
  const mayorista = Number(articulo.precioMayorista ?? (minorista * 0.85).toFixed(2));
  return { minorista, mayorista };
}

function renderizarTablaModalArticulos(filtro) {
  const tbody = document.querySelector("#tablaModalArticulos tbody");
  tbody.innerHTML = "";
  const termino = filtro.toLowerCase().trim();
  const filtrados = listaArticulos.filter(
    (a) =>
      (a.denominacion && a.denominacion.toLowerCase().includes(termino)) ||
      (a.codigo && a.codigo.toLowerCase().includes(termino))
  );
  filtrados.forEach((a) => {
    const precios = obtenerPreciosArticulo(a);
    const tr = document.createElement("tr");
    tr.innerHTML = `
      <td>${a.id}</td>
      <td>${a.codigo || ""}</td>
      <td>${a.denominacion || ""}</td>
      <td>$${precios.minorista.toFixed(2)}</td>
      <td>$${precios.mayorista.toFixed(2)}</td>
      <td><button type="button" class="btn-primary" style="padding: 4px 8px;" onclick="seleccionarArticuloModal(${a.id})">Elegir</button></td>
    `;
    tbody.appendChild(tr);
  });
}

window.seleccionarArticuloModal = (id) => {
  const art = listaArticulos.find((a) => a.id === id);
  document.getElementById("articuloIdHidden").value = String(id);
  document.getElementById("articuloNombreInput").value = art
    ? `${art.codigo ? "[" + art.codigo + "] " : ""}${art.denominacion}`
    : `ID: ${id}`;
  document.getElementById("modalArticulo").style.display = "none";
};

function agregarLinea() {
  if (!facturaEditable) return;

  const articuloId = parseInt(document.getElementById("articuloIdHidden").value);
  const cantidad = parseInt(document.getElementById("cantidadInput").value);
  const tipoLista = document.getElementById("listaPrecioSelect").value;

  if (isNaN(articuloId) || isNaN(cantidad) || cantidad <= 0) {
    alert("Seleccione un artículo válido desde el Explorer y una cantidad mayor a 0.");
    return;
  }

  const art = listaArticulos.find((a) => a.id === articuloId);
  const precios = art ? obtenerPreciosArticulo(art) : { minorista: 0, mayorista: 0 };
  const precioUnitario = tipoLista === "MAYORISTA" ? precios.mayorista : precios.minorista;

  const existente = detallesActuales.find((d) => d.articuloId === articuloId);
  if (existente) {
    existente.cantidad += cantidad;
    existente.precioUnitario = precioUnitario;
  } else {
    detallesActuales.push({
      articuloId,
      nombre: art ? art.denominacion : `Artículo #${articuloId}`,
      precioUnitario,
      cantidad,
    });
  }

  document.getElementById("articuloIdHidden").value = "";
  document.getElementById("articuloNombreInput").value = "";
  document.getElementById("cantidadInput").value = "1";

  actualizarTablaUI();
}

function recalcularPreciosPorLista() {
  const tipoLista = document.getElementById("listaPrecioSelect").value;
  detallesActuales = detallesActuales.map((det) => {
    const art = listaArticulos.find((a) => a.id === det.articuloId);
    if (!art) return det;
    const precios = obtenerPreciosArticulo(art);
    return {
      ...det,
      precioUnitario: tipoLista === "MAYORISTA" ? precios.mayorista : precios.minorista,
    };
  });
  actualizarTablaUI();
}

function actualizarTablaUI() {
  const tbody = document.querySelector("#tablaDetalles tbody");
  tbody.innerHTML = "";
  let total = 0;

  detallesActuales.forEach((detalle, index) => {
    const subtotal = (detalle.precioUnitario || 0) * detalle.cantidad;
    total += subtotal;

    const celdaCantidad = facturaEditable
      ? `<input type="number" min="1" value="${detalle.cantidad}" style="width: 70px; padding: 4px;" onchange="cambiarCantidadLinea(${index}, this.value)" />`
      : `${detalle.cantidad}`;

    const botonQuitar = facturaEditable
      ? `<button type="button" class="btn-danger" style="padding: 4px 8px;" onclick="eliminarLinea(${index})">Quitar</button>`
      : `<span style="color: #9ca3af; font-size: 0.9em;">🔒 Fijo</span>`;

    const tr = document.createElement("tr");
    tr.innerHTML = `
      <td>${detalle.nombre || `ID: ${detalle.articuloId}`}</td>
      <td>$${(detalle.precioUnitario || 0).toFixed(2)}</td>
      <td>${celdaCantidad}</td>
      <td>$${subtotal.toFixed(2)}</td>
      <td>${botonQuitar}</td>
    `;
    tbody.appendChild(tr);
  });

  document.getElementById("totalFacturaEstimado").textContent = total.toFixed(2);
}

window.cambiarCantidadLinea = (index, nuevaCant) => {
  if (!facturaEditable) return;
  const cant = parseInt(nuevaCant);
  if (isNaN(cant) || cant <= 0) {
    alert("La cantidad debe ser al menos 1.");
    actualizarTablaUI();
    return;
  }
  detallesActuales[index].cantidad = cant;
  actualizarTablaUI();
};

window.eliminarLinea = (index) => {
  if (!facturaEditable) return;
  detallesActuales.splice(index, 1);
  actualizarTablaUI();
};

function construirPayloadFactura() {
  const numInput = document.getElementById("numeroFacturaInput").value;
  const numero = numInput ? parseInt(numInput, 10) : undefined;
  const clienteId = parseInt(document.getElementById("clienteIdHidden").value, 10);
  const puntoVentaId = parseInt(document.getElementById("puntoVentaSelect").value, 10);
  const tipoMonedaId = parseInt(document.getElementById("monedaSelect").value, 10);
  const fechaEmision = document.getElementById("fechaEmisionInput").value;
  const tipoComprobanteId = parseInt(document.getElementById("tipoComprobanteSelect").value, 10);
  const listaPrecio = document.getElementById("listaPrecioSelect").value;

  if (!fechaEmision) {
    alert("Debe seleccionar una fecha de emisión.");
    return null;
  }
  if (isNaN(puntoVentaId) || isNaN(tipoMonedaId)) {
    alert("Punto de Venta y Moneda son obligatorios.");
    return null;
  }
  if (detallesActuales.length === 0) {
    alert("La factura debe tener al menos un artículo.");
    return null;
  }

  return {
    numero: !isNaN(numero) ? numero : undefined,
    fechaEmision,
    tipoComprobanteId,
    listaPrecio,
    condicionIvaId: 1,
    clienteId: isNaN(clienteId) ? null : clienteId,
    puntoVentaId,
    tipoMonedaId,
    detalles: detallesActuales.map((d) => ({
      articuloId: d.articuloId,
      cantidad: d.cantidad,
      precioUnitario: d.precioUnitario,
    })),
  };
}

// ALTA (POST)
async function enviarFactura() {
  const payload = construirPayloadFactura();
  if (!payload) return;

  try {
    const respuesta = await facturaService.crearFactura(payload);
    alert(`¡Factura N° ${payload.numero || ""} creada exitosamente! (ID: ${respuesta.id})`);
    document.getElementById("contenedor-panel-factura").style.display = "none";
    await cargarGrillaFacturas();
  } catch (error) {
    alert("Error al guardar: " + error.message);
  }
}

// ACTUALIZACIÓN / EDICIÓN (PUT)
async function actualizarFacturaExistente() {
  if (!facturaSeleccionadaId) return;
  if (!facturaEditable) {
    alert("No se puede editar una factura que ya fue informada a ARCA o está anulada.");
    return;
  }

  const payload = construirPayloadFactura();
  if (!payload) return;

  try {
    await facturaService.actualizarFactura(facturaSeleccionadaId, payload);
    alert("¡Factura actualizada correctamente!");
    await window.seleccionarFactura(facturaSeleccionadaId);
    await cargarGrillaFacturas();
  } catch (error) {
    alert("Error al actualizar: " + error.message);
  }
}

async function cargarGrillaFacturas() {
  const filtroNumero = document.getElementById("filtroNumero").value;
  const filtroCliente = document.getElementById("filtroCliente").value;
  const filtroFechaDesde = document.getElementById("filtroFechaDesde").value;
  const filtroFechaHasta = document.getElementById("filtroFechaHasta").value;
  const filtroEstado = document.getElementById("filtroEstado").value;
  const filtroArca = document.getElementById("filtroArca").value;

  try {
    const pagina = await facturaService.buscarFacturasPaginadas({
      numeroFactura: filtroNumero ? parseInt(filtroNumero) : undefined,
      cliente: filtroCliente || undefined,
      fechaDesde: filtroFechaDesde || undefined,
      fechaHasta: filtroFechaHasta || undefined,
      estado: filtroEstado || undefined,
      informadaArca: filtroArca !== "" ? filtroArca === "true" : undefined,
      page: paginaActual,
      size: 5,
    });
    renderizarTablaFacturas(pagina.contenido);
    document.getElementById("infoPaginacion").textContent =
      `Página ${pagina.pagina + 1} de ${pagina.totalPaginas || 1}`;
  } catch (error) {
    console.error("Error al cargar facturas:", error);
  }
}

function renderizarTablaFacturas(facturas) {
  const tbody = document.querySelector("#tablaGrillaFacturas tbody");
  tbody.innerHTML = "";
  if (facturas.length === 0) {
    tbody.innerHTML =
      '<tr><td colspan="8" style="text-align: center;">No se encontraron facturas.</td></tr>';
    return;
  }
  facturas.forEach((f) => {
    const tr = document.createElement("tr");
    const badgeArca = f.cae
      ? `<span class="tag-arca">CAE: ${f.cae}</span>`
      : `<span class="tag-no-arca">Sin CAE</span>`;
    tr.innerHTML = `
      <td>${f.id}</td>
      <td>${f.numeroFactura ?? f.numero ?? "Borrador"}</td>
      <td>${f.fechaEmision}</td>
      <td>${f.clienteDenominacion}</td>
      <td>$${f.importeTotal.toFixed(2)}</td>
      <td><span class="${f.estado === "ANULADA" ? "tag-anulada" : ""}">${f.estado}</span></td>
      <td>${badgeArca}</td>
      <td><button class="btn-outline" style="padding: 4px 8px;" onclick="seleccionarFactura(${f.id})">Ver / Editar</button></td>
    `;
    tbody.appendChild(tr);
  });
}

window.seleccionarFactura = async (id) => {
  try {
    const factura = await facturaService.obtenerFacturaPorId(id);
    facturaSeleccionadaId = factura.id;

    // Regla de negocio: Solo se puede editar si NO está informada a ARCA (sin CAE) y NO está ANULADA
    const puedeEditar = !factura.cae && factura.estado !== "ANULADA";
    configurarModoEdicion(puedeEditar);

    document.getElementById("tituloFormulario").textContent = puedeEditar
      ? `Editar Factura #${factura.id}`
      : `Visualizar Factura #${factura.id} (${factura.cae ? "Autorizada ARCA" : factura.estado})`;

    document.getElementById("contenedor-panel-factura").style.display = "block";

    const numActual = factura.numero ?? factura.numeroFactura;
    document.getElementById("numeroFacturaInput").value = numActual ? String(numActual) : "";
    document.getElementById("fechaEmisionInput").value = factura.fechaEmision || "";

    if (factura.tipoComprobanteId) {
      document.getElementById("tipoComprobanteSelect").value = String(factura.tipoComprobanteId);
    }
    if (factura.listaPrecio) {
      document.getElementById("listaPrecioSelect").value = factura.listaPrecio;
    }

    const cli = listaClientes.find((c) => c.id === factura.clienteId);
    document.getElementById("clienteIdHidden").value = factura.clienteId ? String(factura.clienteId) : "";
    document.getElementById("clienteNombreInput").value = cli
      ? `${cli.denominacion} (${cli.cuitCuil || "CF"})`
      : factura.clienteDenominacion || "";

    document.getElementById("puntoVentaSelect").value = String(factura.puntoVentaId);
    document.getElementById("monedaSelect").value = String(factura.tipoMonedaId);

    detallesActuales = factura.detalles.map((d) => {
      const art = listaArticulos.find((a) => a.id === d.articuloId);
      return {
        articuloId: d.articuloId,
        nombre: d.descripcion || (art ? art.denominacion : `Artículo #${d.articuloId}`),
        precioUnitario: d.precioUnitario || (art ? obtenerPreciosArticulo(art).minorista : 0),
        cantidad: d.cantidad,
      };
    });
    actualizarTablaUI();

    // Visibilidad de botones según el estado
    document.getElementById("btnGuardarFactura").style.display = "none";
    document.getElementById("btnActualizarFactura").style.display = puedeEditar ? "inline-block" : "none";
    document.getElementById("btnInformarArca").style.display = puedeEditar ? "inline-block" : "none";
    document.getElementById("btnAnularFactura").style.display = puedeEditar ? "inline-block" : "none";
    document.getElementById("btnEliminarFactura").style.display = puedeEditar ? "inline-block" : "none";
    document.getElementById("btnImprimirPdf").style.display = "inline-block";

    document.getElementById("btnAnularFactura").style.display =
      factura.estado !== "ANULADA" ? "inline-block" : "none";

    document.getElementById("btnEliminarFactura").style.display =
      !factura.cae ? "inline-block" : "none";

    document.getElementById("contenedor-panel-factura").scrollIntoView({ behavior: "smooth" });
  } catch (e) {
    alert("Error al cargar la factura: " + e.message);
  }
};

async function informarArca() {
  if (!facturaSeleccionadaId) return;
  if (!confirm("¿Desea enviar este comprobante a ARCA / AFIP para obtener el CAE oficial? Una vez autorizado no podrá editarse.")) return;
  try {
    const res = await facturaService.informarArca(facturaSeleccionadaId);
    alert(`¡Comprobante Autorizado por ARCA!\nCAE: ${res.cae}\nNúmero Oficial: ${res.numeroFactura}`);
    await window.seleccionarFactura(facturaSeleccionadaId);
    await cargarGrillaFacturas();
  } catch (err) {
    alert("Error de ARCA: " + err.message);
  }
}

async function anularFactura() {
  if (!facturaSeleccionadaId) return;
  if (!confirm("¿Confirma la anulación lógica de la factura?")) return;
  try {
    await facturaService.anularFactura(facturaSeleccionadaId);
    alert("Factura anulada.");
    await window.seleccionarFactura(facturaSeleccionadaId);
    await cargarGrillaFacturas();
  } catch (err) {
    alert("Error al anular: " + err.message);
  }
}

async function eliminarFactura() {
  if (!facturaSeleccionadaId) return;
  if (!confirm("¿Eliminar físicamente esta factura?")) return;
  try {
    await facturaService.eliminarFactura(facturaSeleccionadaId);
    alert("Factura eliminada.");
    document.getElementById("contenedor-panel-factura").style.display = "none";
    await cargarGrillaFacturas();
  } catch (err) {
    alert("Error al eliminar: " + err.message);
  }
}

async function imprimirPdf() {
  if (!facturaSeleccionadaId) return;
  try {
    const blob = await facturaService.descargarFacturaIndividualPdf(facturaSeleccionadaId);
    const url = window.URL.createObjectURL(blob);
    window.open(url, "_blank");
  } catch (err) {
    alert("Error al abrir PDF: " + err.message);
  }
}

async function resetearFormulario() {
  facturaSeleccionadaId = null;
  detallesActuales = [];
  configurarModoEdicion(true);

  // Calcula automáticamente el número de la última factura + 1
  const siguienteNumero = await calcularSiguienteNumeroFactura();
  document.getElementById("numeroFacturaInput").value = String(siguienteNumero);

  document.getElementById("fechaEmisionInput").value = new Date().toISOString().split("T")[0];
  document.getElementById("clienteIdHidden").value = "";
  document.getElementById("clienteNombreInput").value = "";
  document.getElementById("articuloIdHidden").value = "";
  document.getElementById("articuloNombreInput").value = "";
  document.getElementById("tipoComprobanteSelect").value = "6";
  document.getElementById("listaPrecioSelect").value = "MINORISTA";

  // Asegurar que Punto de Venta y Moneda queden en la primera opción válida
  const pvSelect = document.getElementById("puntoVentaSelect");
  if (pvSelect && pvSelect.options.length > 1 && !pvSelect.value) {
    pvSelect.selectedIndex = 1;
  }
  const monSelect = document.getElementById("monedaSelect");
  if (monSelect && monSelect.options.length > 1 && !monSelect.value) {
    monSelect.selectedIndex = 1;
  }

  actualizarTablaUI();

  document.getElementById("btnGuardarFactura").style.display = "inline-block";
  document.getElementById("btnActualizarFactura").style.display = "none";
  document.getElementById("btnInformarArca").style.display = "none";
  document.getElementById("btnAnularFactura").style.display = "none";
  document.getElementById("btnEliminarFactura").style.display = "none";
  document.getElementById("btnImprimirPdf").style.display = "none";
}

function limpiarFiltros() {
  document.getElementById("filtroNumero").value = "";
  document.getElementById("filtroCliente").value = "";
  document.getElementById("filtroFechaDesde").value = "";
  document.getElementById("filtroFechaHasta").value = "";
  document.getElementById("filtroEstado").value = "";
  document.getElementById("filtroArca").value = "";
  paginaActual = 0;
  cargarGrillaFacturas();
}
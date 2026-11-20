import { facturaService } from "./facturaService.js";
import {
  FacturaRequestDTO,
  FacturaReporteDTO,
  FacturaVentaDTO,
  ArticuloPrecioDTO,
} from "./types.js";

interface LineaDetalleUI {
  articuloId: number;
  nombre: string;
  precioUnitario: number;
  cantidad: number;
  porcentajeBonificacion: number;
  subtotal: number;
}

let detallesActuales: LineaDetalleUI[] = [];
let facturaSeleccionadaId: number | null = null;
let paginaActual = 0;
let modoSoloLectura = false;
let listaPrecioSeleccionadaPrevia = "2"; // Default Minorista

document.addEventListener("DOMContentLoaded", async () => {
  await cargarVistasModulares();
  await inicializarParametricas();
  configurarEventosGenerales();
  configurarControlListaPrecios();
  configurarControlClienteInput();
  configurarModalesExplorer();
  await cargarGrillaFacturas();
});

// Carga las vistas modulares creadas en la carpeta /views
async function cargarVistasModulares(): Promise<void> {
  try {
    const [historialHtml, formularioHtml, accionesHtml, modalesHtml] =
      await Promise.all([
        fetch("./views/historial.html").then((r) => r.text()),
        fetch("./views/formulario-factura.html").then((r) => r.text()),
        fetch("./views/acciones.html").then((r) => r.text()),
        fetch("./views/modales.html").then((r) => r.text()),
      ]);

    document.getElementById("vista-historial")!.innerHTML = historialHtml;
    document.getElementById("vista-formulario")!.innerHTML = formularioHtml;
    document.getElementById("vista-acciones")!.innerHTML = accionesHtml;
    document.getElementById("vista-modales")!.innerHTML = modalesHtml;
  } catch (error) {
    console.error("Error al cargar las vistas modulares HTML:", error);
  }
}

async function inicializarParametricas() {
  try {
    const [puntos, monedas, listas] = await Promise.all([
      facturaService.obtenerPuntosVenta(),
      facturaService.obtenerTiposMoneda(),
      facturaService.obtenerListasPrecio(),
    ]);

    const pvSelect = document.getElementById(
      "puntoVentaSelect",
    ) as HTMLSelectElement;
    if (pvSelect) {
      pvSelect.innerHTML = puntos
        .map(
          (p) =>
            `<option value="${p.id}">Pto Venta N° ${p.numero} - ${p.descripcion || ""}</option>`,
        )
        .join("");
    }

    const monSelect = document.getElementById(
      "monedaSelect",
    ) as HTMLSelectElement;
    if (monSelect) {
      monSelect.innerHTML = monedas
        .map((m) => `<option value="${m.id}">${m.denominacion}</option>`)
        .join("");
    }

    const lpSelect = document.getElementById(
      "listaPrecioSelect",
    ) as HTMLSelectElement;
    if (lpSelect) {
      lpSelect.innerHTML = listas
        .map((l) => `<option value="${l.id}">${l.denominacion}</option>`)
        .join("");
      if (listas.length > 0) {
        listaPrecioSeleccionadaPrevia = String(listas[0].id);
      }
    }

    const fechaInput = document.getElementById(
      "fechaEmisionInput",
    ) as HTMLInputElement;
    if (fechaInput) {
      fechaInput.value = new Date().toISOString().split("T")[0];
    }
  } catch (e) {
    console.error("Error al cargar paramétricas iniciales", e);
  }
}

function configurarEventosGenerales() {
  document
    .getElementById("btnAbrirNuevaFactura")
    ?.addEventListener("click", abrirFormularioParaCrear);
  document
    .getElementById("btnCerrarFormulario")
    ?.addEventListener("click", cerrarFormulario);

  document
    .getElementById("btnGuardarFactura")
    ?.addEventListener("click", () => guardarOActualizarFactura(false));
  document
    .getElementById("btnActualizarFactura")
    ?.addEventListener("click", () => guardarOActualizarFactura(true));

  // Paginación y Filtros
  document.getElementById("btnFiltrar")?.addEventListener("click", () => {
    paginaActual = 0;
    cargarGrillaFacturas();
  });
  document
    .getElementById("btnLimpiarFiltros")
    ?.addEventListener("click", limpiarFiltros);
  document
    .getElementById("btnPaginaAnterior")
    ?.addEventListener("click", () => {
      if (paginaActual > 0) {
        paginaActual--;
        cargarGrillaFacturas();
      }
    });
  document
    .getElementById("btnPaginaSiguiente")
    ?.addEventListener("click", () => {
      paginaActual++;
      cargarGrillaFacturas();
    });

  document
    .getElementById("btnInformarArca")
    ?.addEventListener("click", informarArca);
  document
    .getElementById("btnAnularFactura")
    ?.addEventListener("click", anularFactura);
  document
    .getElementById("btnEliminarFactura")
    ?.addEventListener("click", eliminarFactura);
  document
    .getElementById("btnImprimirPdf")
    ?.addEventListener("click", imprimirPdf);

  document
    .getElementById("btnAgregarLinea")
    ?.addEventListener("click", agregarLineaDetalle);
}

// -------------------------------------------------------------
// REGLA 1: SI CAMBIA LA LISTA DE PRECIOS, SE TIRAN / LIMPIAN LOS ÍTEMS
// -------------------------------------------------------------
function configurarControlListaPrecios() {
  const lpSelect = document.getElementById(
    "listaPrecioSelect",
  ) as HTMLSelectElement;
  if (!lpSelect) return;

  lpSelect.addEventListener("change", () => {
    if (detallesActuales.length > 0) {
      const confirma = confirm(
        "Al cambiar la Lista de Precios se descartarán los artículos agregados para evitar inconsistencias de precios. ¿Desea continuar?",
      );
      if (confirma) {
        detallesActuales = [];
        actualizarTablaDetallesUI();
        listaPrecioSeleccionadaPrevia = lpSelect.value;
      } else {
        // Revertir a la lista anterior
        lpSelect.value = listaPrecioSeleccionadaPrevia;
      }
    } else {
      listaPrecioSeleccionadaPrevia = lpSelect.value;
    }
  });
}

// -------------------------------------------------------------
// REGLA 2: BORRADO DEL CLIENTE DIRECTAMENTE EN EL INPUT
// -------------------------------------------------------------
function configurarControlClienteInput() {
  const cliInput = document.getElementById(
    "clienteNombreInput",
  ) as HTMLInputElement;
  const cliHidden = document.getElementById(
    "clienteIdHidden",
  ) as HTMLInputElement;
  const btnLimpiar = document.getElementById(
    "btnLimpiarCliente",
  ) as HTMLButtonElement;

  const limpiarCliente = () => {
    cliInput.value = "";
    cliHidden.value = "";
  };

  btnLimpiar?.addEventListener("click", limpiarCliente);

  // Si el usuario borra el texto con Backspace/Delete o deja en blanco el campo
  cliInput?.addEventListener("input", () => {
    if (!cliInput.value.trim()) {
      cliHidden.value = "";
    }
  });

  cliInput?.addEventListener("keydown", (e) => {
    if (e.key === "Backspace" || e.key === "Delete") {
      cliHidden.value = "";
    }
  });
}

function abrirFormularioParaCrear() {
  facturaSeleccionadaId = null;
  detallesActuales = [];
  limpiarCamposFormulario();

  document.getElementById("tituloFormulario")!.textContent =
    "Nueva Factura de Venta";
  document.getElementById("btnGuardarFactura")!.style.display = "inline-block";
  document.getElementById("btnActualizarFactura")!.style.display = "none";
  document.getElementById("btnInformarArca")!.style.display = "none";
  document.getElementById("btnAnularFactura")!.style.display = "none";
  document.getElementById("btnEliminarFactura")!.style.display = "none";
  document.getElementById("btnImprimirPdf")!.style.display = "none";
  document.getElementById("bannerSoloLectura")!.style.display = "none";

  habilitarEdicionFormulario(true);
  const panel = document.getElementById("contenedor-panel-factura")!;
  panel.style.display = "block";
  panel.scrollIntoView({ behavior: "smooth" });
}

function cerrarFormulario() {
  document.getElementById("contenedor-panel-factura")!.style.display = "none";
}

function limpiarCamposFormulario() {
  (document.getElementById("numeroFacturaInput") as HTMLInputElement).value =
    "";
  (document.getElementById("clienteIdHidden") as HTMLInputElement).value = "";
  (document.getElementById("clienteNombreInput") as HTMLInputElement).value =
    "";
  (document.getElementById("articuloIdHidden") as HTMLInputElement).value = "";
  (document.getElementById("articuloNombreInput") as HTMLInputElement).value =
    "";
  (document.getElementById("cantidadInput") as HTMLInputElement).value = "1";
  (document.getElementById("descuentoInput") as HTMLInputElement).value = "0";
  (document.getElementById("fechaEmisionInput") as HTMLInputElement).value =
    new Date().toISOString().split("T")[0];
  actualizarTablaDetallesUI();
}

function habilitarEdicionFormulario(habilitar: boolean) {
  modoSoloLectura = !habilitar;
  actualizarTablaDetallesUI();

  const inputs = document.querySelectorAll(
    "#seccionFormulario select, #seccionFormulario input:not([readonly]), #barraAgregarArticulo input",
  );
  inputs.forEach((el) => ((el as HTMLInputElement).disabled = !habilitar));

  (
    document.getElementById("btnAbrirModalCliente") as HTMLButtonElement
  ).disabled = !habilitar;
  (document.getElementById("btnLimpiarCliente") as HTMLButtonElement).disabled =
    !habilitar;
  (
    document.getElementById("btnAbrirModalArticulo") as HTMLButtonElement
  ).disabled = !habilitar;
  (document.getElementById("btnAgregarLinea") as HTMLButtonElement).disabled =
    !habilitar;

  const barraAgregar = document.getElementById("barraAgregarArticulo")!;
  barraAgregar.style.display = habilitar ? "flex" : "none";
}

// ------------------------------------------------------------- EXPLORERS MODALES
function configurarModalesExplorer() {
  // Explorer Clientes
  const modalCli = document.getElementById("modalCliente")!;
  document
    .getElementById("btnAbrirModalCliente")
    ?.addEventListener("click", () => {
      modalCli.style.display = "flex";
      ejecutarBusquedaClientes();
    });
  document
    .getElementById("btnCerrarModalCliente")
    ?.addEventListener("click", () => {
      modalCli.style.display = "none";
    });

  const buscarClientes = () => {
    const nombre =
      (
        document.getElementById("filtroModalClienteNombre") as HTMLInputElement
      )?.value.trim() || "";
    const cuit =
      (
        document.getElementById("filtroModalClienteCuit") as HTMLInputElement
      )?.value.trim() || "";
    ejecutarBusquedaClientes(nombre || cuit);
  };

  document
    .getElementById("btnBuscarModalCliente")
    ?.addEventListener("click", buscarClientes);
  ["filtroModalClienteNombre", "filtroModalClienteCuit"].forEach((id) => {
    document.getElementById(id)?.addEventListener("keydown", (e) => {
      if ((e as KeyboardEvent).key === "Enter") buscarClientes();
    });
  });

  // Explorer Artículos
  const modalArt = document.getElementById("modalArticulo")!;
  document
    .getElementById("btnAbrirModalArticulo")
    ?.addEventListener("click", () => {
      const lpSelect = document.getElementById(
        "listaPrecioSelect",
      ) as HTMLSelectElement;
      if (!lpSelect || !lpSelect.value) {
        alert("Debe seleccionar primero una Lista de Precios.");
        return;
      }
      modalArt.style.display = "flex";
      ejecutarBusquedaArticulos();
    });
  document
    .getElementById("btnCerrarModalArticulo")
    ?.addEventListener("click", () => {
      modalArt.style.display = "none";
    });
  const buscarArticulos = () => {
    const filtro =
      (
        document.getElementById("filtroModalArticulo") as HTMLInputElement
      )?.value.trim() || "";
    ejecutarBusquedaArticulos(filtro);
  };
  document
    .getElementById("btnBuscarModalArticulo")
    ?.addEventListener("click", buscarArticulos);
  document
    .getElementById("filtroModalArticulo")
    ?.addEventListener("keydown", (e) => {
      if ((e as KeyboardEvent).key === "Enter") buscarArticulos();
    });
}

async function ejecutarBusquedaClientes(filtro: string = "") {
  const tbody = document.querySelector(
    "#tablaModalClientes tbody",
  ) as HTMLTableSectionElement;
  tbody.innerHTML =
    '<tr><td colspan="4" style="text-align:center;">Buscando clientes en base de datos...</td></tr>';

  try {
    const clientes = await facturaService.buscarClientesExplorer(filtro);
    if (clientes.length === 0) {
      tbody.innerHTML =
        '<tr><td colspan="4" style="text-align:center;">No se encontraron clientes.</td></tr>';
      return;
    }

    tbody.innerHTML = clientes
      .map(
        (c) => `
        <tr>
          <td>${c.id}</td>
          <td>${c.denominacion || ""}</td>
          <td>${c.cuitCuil || "Consumidor Final"}</td>
          <td>
            <button type="button" class="btn-primary" style="padding: 4px 8px;" onclick="seleccionarClienteExplorer(${c.id}, '${(c.denominacion || "").replace(/'/g, "\\'")}')">
              Elegir
            </button>
          </td>
        </tr>
      `,
      )
      .join("");
  } catch (err: any) {
    tbody.innerHTML = `<tr><td colspan="4" style="color:red; text-align:center;">Error: ${err.message}</td></tr>`;
  }
}

(window as any).seleccionarClienteExplorer = (
  id: number,
  denominacion: string,
) => {
  (document.getElementById("clienteIdHidden") as HTMLInputElement).value =
    String(id);
  (document.getElementById("clienteNombreInput") as HTMLInputElement).value =
    `${denominacion} (ID: ${id})`;
  document.getElementById("modalCliente")!.style.display = "none";
};

async function ejecutarBusquedaArticulos(filtro: string = "") {
  const lpSelect = document.getElementById(
    "listaPrecioSelect",
  ) as HTMLSelectElement;
  const lpId = parseInt(lpSelect.value) || 1;
  const nombreLista =
    lpSelect.options[lpSelect.selectedIndex]?.text || "Lista de Precios";

  // Mostramos en el título del Explorer con qué lista se está cotizando
  const tituloModal = document.querySelector("#modalArticulo .modal-header h4");
  if (tituloModal) {
    tituloModal.textContent = `Explorer de Artículos — ${nombreLista}`;
  }

  const tbody = document.querySelector(
    "#tablaModalArticulos tbody",
  ) as HTMLTableSectionElement;
  tbody.innerHTML =
    '<tr><td colspan="4" style="text-align:center;">Buscando artículos en catálogo...</td></tr>';

  try {
    const articulos = await facturaService.buscarArticulosExplorer(
      lpId,
      filtro,
    );
    if (articulos.length === 0) {
      tbody.innerHTML =
        '<tr><td colspan="4" style="text-align:center;">No se encontraron artículos para esta lista.</td></tr>';
      return;
    }

    tbody.innerHTML = articulos
      .map(
        (a) => `
        <tr>
          <td><strong>${a.codigo}</strong></td>
          <td>${a.denominacion}</td>
          <td><strong style="color: #047857;">$${a.precioVenta.toFixed(2)}</strong></td>
          <td>
            <button type="button" class="btn-primary" style="padding: 4px 10px;" onclick="seleccionarArticuloExplorer(${a.articuloId}, '${a.codigo}', '${a.denominacion.replace(/'/g, "\\'")}', ${a.precioVenta})">
              Elegir
            </button>
          </td>
        </tr>
      `,
      )
      .join("");
  } catch (err: any) {
    tbody.innerHTML = `<tr><td colspan="4" style="color:red; text-align:center;">Error: ${err.message}</td></tr>`;
  }
}

(window as any).seleccionarArticuloExplorer = (
  id: number,
  codigo: string,
  denominacion: string,
  precio: number,
) => {
  const artInput = document.getElementById(
    "articuloNombreInput",
  ) as HTMLInputElement;
  artInput.value = `[${codigo}] ${denominacion} - $${precio.toFixed(2)}`;
  artInput.dataset.precio = String(precio);
  (document.getElementById("articuloIdHidden") as HTMLInputElement).value =
    String(id);
  document.getElementById("modalArticulo")!.style.display = "none";
};

// -------------------------------------------------------------
// REGLA 3 Y 4: DESCUENTO + MODIFICACIÓN DE CANTIDAD EN BORRADOR
// -------------------------------------------------------------
function agregarLineaDetalle() {
  if (modoSoloLectura) return;
  const artId = parseInt(
    (document.getElementById("articuloIdHidden") as HTMLInputElement).value,
  );
  const artNombreInput = document.getElementById(
    "articuloNombreInput",
  ) as HTMLInputElement;
  const precio = parseFloat(artNombreInput.dataset.precio || "0");
  const cantidad = parseInt(
    (document.getElementById("cantidadInput") as HTMLInputElement).value,
  );
  const descuento =
    parseFloat(
      (document.getElementById("descuentoInput") as HTMLInputElement).value,
    ) || 0;

  if (
    isNaN(artId) ||
    isNaN(cantidad) ||
    cantidad <= 0 ||
    isNaN(precio) ||
    precio <= 0
  ) {
    alert(
      "Debe seleccionar un artículo válido con el Explorer y una cantidad mayor a 0.",
    );
    return;
  }

  const neto = cantidad * precio * (1 - descuento / 100);
  const subtotal = neto * 1.21;

  detallesActuales.push({
    articuloId: artId,
    nombre: artNombreInput.value,
    precioUnitario: precio,
    cantidad,
    porcentajeBonificacion: descuento,
    subtotal,
  });

  artNombreInput.value = "";
  artNombreInput.dataset.precio = "";
  (document.getElementById("articuloIdHidden") as HTMLInputElement).value = "";
  (document.getElementById("descuentoInput") as HTMLInputElement).value = "0";
  actualizarTablaDetallesUI();
}

function actualizarTablaDetallesUI() {
  const tbody = document.querySelector(
    "#tablaDetalles tbody",
  ) as HTMLTableSectionElement;
  tbody.innerHTML = "";

  let total = 0;
  detallesActuales.forEach((det, index) => {
    total += det.subtotal;
    const tr = document.createElement("tr");

    // Si está en borrador (editable), la cantidad es un input interactivo
    const celdaCantidad = modoSoloLectura
      ? `<td>${det.cantidad}</td>`
      : `<td>
           <input type="number" min="1" value="${det.cantidad}" 
                  style="width: 70px; padding: 4px;" 
                  onchange="modificarCantidadLinea(${index}, this.value)" />
         </td>`;

    tr.innerHTML = `
      <td>${det.nombre}</td>
      <td>$${det.precioUnitario.toFixed(2)}</td>
      ${celdaCantidad}
      <td>${det.porcentajeBonificacion > 0 ? `${det.porcentajeBonificacion}%` : "-"}</td>
      <td>$${det.subtotal.toFixed(2)}</td>
      <td>${
        modoSoloLectura
          ? "—"
          : `<button type="button" class="btn-danger" style="padding: 3px 8px;" onclick="quitarLinea(${index})">✕</button>`
      }</td>
    `;
    tbody.appendChild(tr);
  });

  const totalEl = document.getElementById("totalFacturaEstimado");
  if (totalEl) totalEl.textContent = total.toFixed(2);
}

// Función interactiva para modificar la cantidad de un ítem en borrador
(window as any).modificarCantidadLinea = (
  index: number,
  nuevaCantidadStr: string,
) => {
  if (modoSoloLectura) return;
  const nuevaCant = parseInt(nuevaCantidadStr);
  if (isNaN(nuevaCant) || nuevaCant <= 0) {
    alert("La cantidad debe ser mayor a 0.");
    actualizarTablaDetallesUI();
    return;
  }

  const det = detallesActuales[index];
  det.cantidad = nuevaCant;
  const neto =
    det.cantidad * det.precioUnitario * (1 - det.porcentajeBonificacion / 100);
  det.subtotal = neto * 1.21;
  actualizarTablaDetallesUI();
};

(window as any).quitarLinea = (index: number) => {
  if (modoSoloLectura) return;
  detallesActuales.splice(index, 1);
  actualizarTablaDetallesUI();
};

// ------------------------------------------------------------- GUARDAR Y ACTUALIZAR
async function guardarOActualizarFactura(esActualizacion: boolean) {
  const pvId = parseInt(
    (document.getElementById("puntoVentaSelect") as HTMLSelectElement).value,
  );
  const monId = parseInt(
    (document.getElementById("monedaSelect") as HTMLSelectElement).value,
  );
  const lpId =
    parseInt(
      (document.getElementById("listaPrecioSelect") as HTMLSelectElement).value,
    ) || 1;
  const letra =
    (document.getElementById("tipoComprobanteSelect") as HTMLSelectElement)
      .value || "B";
  const fecha = (
    document.getElementById("fechaEmisionInput") as HTMLInputElement
  ).value;
  const clienteIdVal = (
    document.getElementById("clienteIdHidden") as HTMLInputElement
  ).value;

  if (!fecha || isNaN(pvId)) {
    alert("Punto de Venta y Fecha de Emisión son obligatorios.");
    return;
  }
  if (detallesActuales.length === 0) {
    alert("La factura debe tener al menos un artículo cargado.");
    return;
  }

  const payload: FacturaRequestDTO = {
    fechaEmision: fecha,
    puntoVentaId: pvId,
    condicionIvaId: 1,
    tipoMonedaId: monId || 1,
    listaPrecioId: lpId,
    letra: letra,
    clienteId: clienteIdVal ? parseInt(clienteIdVal) : null,
    detalles: detallesActuales.map((d) => ({
      articuloId: d.articuloId,
      cantidad: d.cantidad,
      porcentajeBonificacion: d.porcentajeBonificacion,
    })),
  };

  try {
    if (esActualizacion && facturaSeleccionadaId) {
      await facturaService.actualizarFactura(facturaSeleccionadaId, payload);
      alert("¡Factura actualizada exitosamente!");
    } else {
      const creada = await facturaService.crearFactura(payload);
      alert(`¡Factura guardada exitosamente con ID: ${creada.id}!`);
    }
    cerrarFormulario();
    await cargarGrillaFacturas();
  } catch (err: any) {
    alert("Error: " + err.message);
  }
}

// -------------------------------------------------------------
// REGLA 5: PAGINACIÓN EXACTA (Página X de Y - Total: N facturas)
// -------------------------------------------------------------
async function cargarGrillaFacturas() {
  const filtroNumero = (
    document.getElementById("filtroNumero") as HTMLInputElement
  )?.value;
  const filtroCliente = (
    document.getElementById("filtroCliente") as HTMLInputElement
  )?.value;
  const filtroFechaDesde = (
    document.getElementById("filtroFechaDesde") as HTMLInputElement
  )?.value;
  const filtroFechaHasta = (
    document.getElementById("filtroFechaHasta") as HTMLInputElement
  )?.value;
  const filtroEstado = (
    document.getElementById("filtroEstado") as HTMLSelectElement
  )?.value;
  const filtroArca = (
    document.getElementById("filtroArca") as HTMLSelectElement
  )?.value;

  try {
    const pagina = await facturaService.buscarFacturasPaginadas({
      numeroFactura: filtroNumero ? parseInt(filtroNumero) : undefined,
      cliente: filtroCliente || undefined,
      fechaDesde: filtroFechaDesde || undefined,
      fechaHasta: filtroFechaHasta || undefined,
      estado: filtroEstado || undefined,
      informadaArca:
        filtroArca !== "" && filtroArca !== undefined
          ? filtroArca === "true"
          : undefined,
      page: paginaActual,
      size: 5,
    });

    renderizarTablaFacturas(pagina.contenido);

    // Cálculo y renderizado de la barra de paginación
    const totalPaginasCalculadas = Math.max(1, pagina.totalPaginas);
    const pagSpan = document.getElementById("infoPaginacion");
    if (pagSpan) {
      pagSpan.textContent = `Página ${pagina.pagina + 1} de ${totalPaginasCalculadas} (Total: ${pagina.totalElementos} facturas)`;
    }

    const btnAnt = document.getElementById(
      "btnPaginaAnterior",
    ) as HTMLButtonElement;
    const btnSig = document.getElementById(
      "btnPaginaSiguiente",
    ) as HTMLButtonElement;

    if (btnAnt) btnAnt.disabled = paginaActual <= 0;
    if (btnSig) btnSig.disabled = paginaActual >= totalPaginasCalculadas - 1;
  } catch (error) {
    console.error("Error al cargar la grilla de facturas:", error);
  }
}

function renderizarTablaFacturas(facturas: FacturaReporteDTO[]) {
  const tbody = document.querySelector(
    "#tablaGrillaFacturas tbody",
  ) as HTMLTableSectionElement;
  if (!tbody) return;
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
      <td>${f.numeroFactura || "Borrador"}</td>
      <td>${f.fechaEmision}</td>
      <td>${f.clienteDenominacion}</td>
      <td>$${f.importeTotal.toFixed(2)}</td>
      <td><span class="${f.estado === "ANULADA" ? "tag-anulada" : ""}">${f.estado}</span></td>
      <td>${badgeArca}</td>
      <td>
        <button class="btn-outline" style="padding: 4px 8px;" onclick="seleccionarFactura(${f.id})">
          Ver / Gestionar
        </button>
      </td>
    `;
    tbody.appendChild(tr);
  });
}

(window as any).seleccionarFactura = async (id: number) => {
  try {
    const f = await facturaService.obtenerFacturaPorId(id);
    facturaSeleccionadaId = f.id;

    (document.getElementById("numeroFacturaInput") as HTMLInputElement).value =
      f.numero ? String(f.numero) : "Borrador";
    (document.getElementById("fechaEmisionInput") as HTMLInputElement).value =
      f.fechaEmision;
    (document.getElementById("puntoVentaSelect") as HTMLSelectElement).value =
      String(f.puntoVentaId);
    (document.getElementById("monedaSelect") as HTMLSelectElement).value =
      String(f.tipoMonedaId);

    if (f.clienteId) {
      (document.getElementById("clienteIdHidden") as HTMLInputElement).value =
        String(f.clienteId);
      (
        document.getElementById("clienteNombreInput") as HTMLInputElement
      ).value = `${f.clienteDenominacion || "Cliente"} (ID: ${f.clienteId})`;
    } else {
      (document.getElementById("clienteIdHidden") as HTMLInputElement).value =
        "";
      (
        document.getElementById("clienteNombreInput") as HTMLInputElement
      ).value = "";
    }

    detallesActuales = f.detalles.map((d) => ({
      articuloId: d.articuloId,
      nombre: d.descripcion || d.articuloDenominacion,
      precioUnitario: d.precioUnitario,
      cantidad: d.cantidad,
      porcentajeBonificacion: d.porcentajeBonificacion || 0,
      subtotal: d.importeSubtotal,
    }));
    actualizarTablaDetallesUI();

    document.getElementById("tituloFormulario")!.textContent =
      `Factura #${f.numero || "Borrador"} (ID: ${f.id})`;
    document.getElementById("contenedor-panel-factura")!.style.display =
      "block";

    const tieneCae = Boolean(f.cae);
    const esAnulada = f.estado === "ANULADA";

    habilitarEdicionFormulario(!tieneCae && !esAnulada);

    document.getElementById("btnGuardarFactura")!.style.display = "none";
    document.getElementById("btnActualizarFactura")!.style.display =
      !tieneCae && !esAnulada ? "inline-block" : "none";
    document.getElementById("btnInformarArca")!.style.display =
      !tieneCae && !esAnulada ? "inline-block" : "none";
    document.getElementById("btnEliminarFactura")!.style.display =
      !f.numero && !tieneCae ? "inline-block" : "none";
    document.getElementById("btnImprimirPdf")!.style.display = "inline-block";

    // BOTÓN ANULAR: Si tiene CAE, se muestra bloqueado/deshabilitado
    const btnAnular = document.getElementById(
      "btnAnularFactura",
    ) as HTMLButtonElement;
    if (btnAnular) {
      if (esAnulada) {
        btnAnular.style.display = "none";
      } else if (tieneCae) {
        btnAnular.style.display = "inline-block";
        btnAnular.disabled = true;
        btnAnular.title =
          "No se puede anular un comprobante que ya posee CAE de ARCA.";
        btnAnular.style.opacity = "0.5";
        btnAnular.style.cursor = "not-allowed";
      } else {
        btnAnular.style.display = "inline-block";
        btnAnular.disabled = false;
        btnAnular.title = "";
        btnAnular.style.opacity = "1";
        btnAnular.style.cursor = "pointer";
      }
    }

    const banner = document.getElementById("bannerSoloLectura")!;
    banner.style.display = tieneCae || esAnulada ? "block" : "none";

    document
      .getElementById("contenedor-panel-factura")!
      .scrollIntoView({ behavior: "smooth" });
  } catch (e: any) {
    alert("Error al cargar factura: " + e.message);
  }
};

async function informarArca() {
  if (!facturaSeleccionadaId) return;
  if (
    !confirm(
      "¿Desea enviar este comprobante a ARCA / AFIP para obtener el CAE oficial?",
    )
  )
    return;

  try {
    const res = await facturaService.informarArca(facturaSeleccionadaId);
    alert(
      `¡Comprobante Autorizado por ARCA!\nCAE: ${res.cae}\nNúmero: ${res.numeroFactura}`,
    );
    await (window as any).seleccionarFactura(facturaSeleccionadaId);
    await cargarGrillaFacturas();
  } catch (err: any) {
    alert("Error de ARCA: " + err.message);
  }
}

async function anularFactura() {
  if (!facturaSeleccionadaId) return;

  const btnAnular = document.getElementById(
    "btnAnularFactura",
  ) as HTMLButtonElement;
  if (btnAnular && btnAnular.disabled) {
    alert(
      "Operación denegada: No se puede anular una factura que ya posee CAE oficial de ARCA.",
    );
    return;
  }

  if (!confirm("¿Confirma la anulación lógica de la factura?")) return;
  try {
    await facturaService.anularFactura(facturaSeleccionadaId);
    alert("Factura anulada con éxito.");
    await (window as any).seleccionarFactura(facturaSeleccionadaId);
    await cargarGrillaFacturas();
  } catch (err: any) {
    alert("Error al anular: " + err.message);
  }
}

async function eliminarFactura() {
  if (!facturaSeleccionadaId) return;
  if (!confirm("¿Eliminar físicamente el borrador?")) return;
  try {
    await facturaService.eliminarFactura(facturaSeleccionadaId);
    alert("Factura eliminada.");
    cerrarFormulario();
    await cargarGrillaFacturas();
  } catch (err: any) {
    alert("Error al eliminar: " + err.message);
  }
}

async function imprimirPdf() {
  if (!facturaSeleccionadaId) return;
  try {
    const blob = await facturaService.descargarFacturaIndividualPdf(
      facturaSeleccionadaId,
    );
    const url = window.URL.createObjectURL(blob);
    window.open(url, "_blank");
  } catch (err: any) {
    alert("Error al abrir PDF: " + err.message);
  }
}

function limpiarFiltros() {
  (document.getElementById("filtroNumero") as HTMLInputElement).value = "";
  (document.getElementById("filtroCliente") as HTMLInputElement).value = "";
  (document.getElementById("filtroFechaDesde") as HTMLInputElement).value = "";
  (document.getElementById("filtroFechaHasta") as HTMLInputElement).value = "";
  (document.getElementById("filtroEstado") as HTMLSelectElement).value = "";
  (document.getElementById("filtroArca") as HTMLSelectElement).value = "";
  paginaActual = 0;
  cargarGrillaFacturas();
}

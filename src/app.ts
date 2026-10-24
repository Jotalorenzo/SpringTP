import { facturaService } from "./facturaService.js";
import {
  FacturaRequestDTO,
  FacturaDetalleRequestDTO,
  FacturaVentaDTO,
} from "./types.js";

let detallesActuales: FacturaDetalleRequestDTO[] = [];
let facturaCargadaActual: FacturaVentaDTO | null = null;

document.addEventListener("DOMContentLoaded", async () => {
  await inicializarFormulario();
  configurarEventos();
});

async function inicializarFormulario() {
  try {
    const [clientes, puntosVenta, monedas, articulos] = await Promise.all([
      facturaService.obtenerClientes(),
      facturaService.obtenerPuntosVenta(),
      facturaService.obtenerTiposMoneda(),
      facturaService.obtenerArticulos(),
    ]);

    llenarSelect("clienteSelect", clientes, "id", "denominacion");
    llenarSelect("puntoVentaSelect", puntosVenta, "id", "descripcion");
    llenarSelect("monedaSelect", monedas, "id", "denominacion");
    llenarSelect("articuloSelect", articulos, "id", "denominacion");
  } catch (error) {
    console.error("Error al cargar paramétricas:", error);
  }
}

function llenarSelect(
  selectId: string,
  items: any[],
  valueProp: string,
  textProp: string,
) {
  const select = document.getElementById(selectId) as HTMLSelectElement;
  select.innerHTML = '<option value="">Seleccione una opción...</option>';
  items.forEach((item) => {
    const option = document.createElement("option");
    option.value = item[valueProp];
    option.textContent = item[textProp] || `ID: ${item[valueProp]}`;
    select.appendChild(option);
  });
}

function configurarEventos() {
  document
    .getElementById("btnAgregarLinea")
    ?.addEventListener("click", agregarLinea);
  document
    .getElementById("btnGuardarFactura")
    ?.addEventListener("click", crearFactura);
  document
    .getElementById("btnActualizarFactura")
    ?.addEventListener("click", actualizarFactura);
  document
    .getElementById("btnAnularFactura")
    ?.addEventListener("click", anularFactura);
  document
    .getElementById("btnEliminarFactura")
    ?.addEventListener("click", eliminarFactura);
  document
    .getElementById("btnBuscarFactura")
    ?.addEventListener("click", cargarFacturaPorId);
  document
    .getElementById("btnNuevaFactura")
    ?.addEventListener("click", resetearAModoNuevo);
}

function agregarLinea() {
  const articuloSelect = document.getElementById(
    "articuloSelect",
  ) as HTMLSelectElement;
  const cantidadInput = document.getElementById(
    "cantidadInput",
  ) as HTMLInputElement;

  const articuloId = parseInt(articuloSelect.value);
  const cantidad = parseFloat(cantidadInput.value);

  if (isNaN(articuloId) || isNaN(cantidad) || cantidad <= 0) {
    alert("Seleccione un artículo válido y cantidad mayor a 0.");
    return;
  }

  detallesActuales.push({ articuloId, cantidad, porcentajeBonificacion: 0.0 });
  actualizarTablaUI();
}

function actualizarTablaUI() {
  const tbody = document.querySelector(
    "#tablaDetalles tbody",
  ) as HTMLTableSectionElement;
  tbody.innerHTML = "";

  const bloqueado =
    facturaCargadaActual &&
    (facturaCargadaActual.cae || facturaCargadaActual.estado === "ANULADA");

  detallesActuales.forEach((detalle, index) => {
    const tr = document.createElement("tr");
    tr.innerHTML = `
            <td>${detalle.articuloId}</td>
            <td>${detalle.cantidad}</td>
            <td>${bloqueado ? "Bloqueado" : `<button type="button" onclick="eliminarLinea(${index})">Quitar</button>`}</td>
        `;
    tbody.appendChild(tr);
  });
}

(window as any).eliminarLinea = (index: number) => {
  detallesActuales.splice(index, 1);
  actualizarTablaUI();
};

function armarPayload(): FacturaRequestDTO | null {
  const clienteId = parseInt(
    (document.getElementById("clienteSelect") as HTMLSelectElement).value,
  );
  const puntoVentaId = parseInt(
    (document.getElementById("puntoVentaSelect") as HTMLSelectElement).value,
  );
  const tipoMonedaId = parseInt(
    (document.getElementById("monedaSelect") as HTMLSelectElement).value,
  );
  const numeroInput = (
    document.getElementById("numeroFacturaInput") as HTMLInputElement
  ).value;

  if (isNaN(puntoVentaId) || isNaN(tipoMonedaId)) {
    alert("Punto de Venta y Moneda son obligatorios.");
    return null;
  }

  if (detallesActuales.length === 0) {
    alert("La factura debe tener al menos un artículo.");
    return null;
  }

  const hoy = new Date().toISOString().split("T")[0];
  const numero = numeroInput
    ? parseInt(numeroInput)
    : Math.floor(1000 + Math.random() * 9000);

  return {
    numero,
    fechaEmision: facturaCargadaActual
      ? facturaCargadaActual.fechaEmision
      : hoy,
    puntoVentaId,
    condicionIvaId: 1,
    tipoMonedaId,
    clienteId: isNaN(clienteId) ? null : clienteId,
    detalles: detallesActuales,
  };
}

async function crearFactura() {
  const payload = armarPayload();
  if (!payload) return;

  try {
    const res = await facturaService.crearFactura(payload);
    alert(
      `¡Factura generada exitosamente con Nro: ${res.numeroFactura}! Total: $${res.importeTotal}`,
    );
    resetearAModoNuevo();
  } catch (error: any) {
    alert(error.message);
  }
}

async function actualizarFactura() {
  if (!facturaCargadaActual) return;
  const payload = armarPayload();
  if (!payload) return;

  try {
    await facturaService.actualizarFactura(facturaCargadaActual.id, payload);
    alert(`¡Factura #${facturaCargadaActual.id} actualizada correctamente!`);
    resetearAModoNuevo();
  } catch (error: any) {
    alert(error.message);
  }
}

async function anularFactura() {
  if (!facturaCargadaActual) return;
  if (
    !confirm(
      `¿Seguro que desea anular la factura ID ${facturaCargadaActual.id}?`,
    )
  )
    return;

  try {
    await facturaService.anularFactura(facturaCargadaActual.id);
    alert(`Factura anulada exitosamente.`);
    resetearAModoNuevo();
  } catch (error: any) {
    alert(error.message);
  }
}

async function eliminarFactura() {
  if (!facturaCargadaActual) return;
  if (
    !confirm(
      `¿Seguro que desea eliminar físicamente la factura ID ${facturaCargadaActual.id}?`,
    )
  )
    return;

  try {
    await facturaService.eliminarFactura(facturaCargadaActual.id);
    alert(`Factura eliminada.`);
    resetearAModoNuevo();
  } catch (error: any) {
    alert(error.message);
  }
}

// Carga una factura y aplica las reglas de visibilidad y bloqueo
async function cargarFacturaPorId() {
  const input = document.getElementById("buscarIdInput") as HTMLInputElement;
  const id = parseInt(input.value);
  if (isNaN(id)) return alert("Ingrese un ID válido.");

  try {
    const f = await facturaService.obtenerFacturaPorId(id);
    facturaCargadaActual = f;

    (document.getElementById("numeroFacturaInput") as HTMLInputElement).value =
      f.numero?.toString() || "";
    (document.getElementById("clienteSelect") as HTMLSelectElement).value =
      f.clienteId?.toString() || "";
    (document.getElementById("puntoVentaSelect") as HTMLSelectElement).value =
      f.puntoVentaId?.toString() || "";
    (document.getElementById("monedaSelect") as HTMLSelectElement).value =
      f.tipoMonedaId?.toString() || "";

    detallesActuales = f.detalles.map((d) => ({
      articuloId: d.articuloId,
      cantidad: d.cantidad,
      porcentajeBonificacion: 0.0,
    }));

    aplicarReglasDeNegocioVisuales(f);
  } catch (error: any) {
    alert(error.message);
  }
}

function aplicarReglasDeNegocioVisuales(f: FacturaVentaDTO) {
  const alerta = document.getElementById("alertaEstado")!;
  const btnGuardar = document.getElementById("btnGuardarFactura")!;
  const btnActualizar = document.getElementById("btnActualizarFactura")!;
  const btnAnular = document.getElementById("btnAnularFactura")!;
  const btnEliminar = document.getElementById("btnEliminarFactura")!;
  const btnAgregar = document.getElementById("btnAgregarLinea")!;

  btnGuardar.style.display = "none";
  alerta.style.display = "block";

  const tieneCae = f.cae && f.cae.trim().length > 0;
  const tieneNumero = f.numero && f.numero > 0;
  const esAnulada = f.estado === "ANULADA";

  if (esAnulada) {
    alerta.className = "badge-anulada";
    alerta.textContent = `FACTURA ANULADA. Bloqueada para cualquier modificación.`;
    btnActualizar.style.display = "none";
    btnAnular.style.display = "none";
    btnEliminar.style.display = "none";
    bloquearControles(true);
  } else if (tieneCae) {
    alerta.className = "badge-arca";
    alerta.textContent = `INFORMADA A ARCA (CAE: ${f.cae}). Modificación bloqueada. Solo se permite Anular.`;
    btnActualizar.style.display = "none";
    btnEliminar.style.display = "none"; // PROHIBIDO ELIMINAR
    btnAnular.style.display = "inline-block"; // SOLO ANULAR
    bloquearControles(true);
  } else if (tieneNumero) {
    alerta.style.background = "#e0f2fe";
    alerta.style.color = "#0369a1";
    alerta.textContent = `Factura emitida con número #${f.numero}. No se puede eliminar, solo Anular o Actualizar.`;
    btnActualizar.style.display = "inline-block";
    btnAnular.style.display = "inline-block";
    btnEliminar.style.display = "none"; // PROHIBIDO ELIMINAR SI TIENE NÚMERO
    bloquearControles(false);
  } else {
    // Es un borrador sin número ni CAE
    alerta.style.background = "#fef3c7";
    alerta.style.color = "#92400e";
    alerta.textContent = `Borrador / Sin número asignado. Se puede modificar o eliminar físicamente.`;
    btnActualizar.style.display = "inline-block";
    btnEliminar.style.display = "inline-block"; // PERMITIDO ELIMINAR
    btnAnular.style.display = "none";
    bloquearControles(false);
  }

  actualizarTablaUI();
}

function bloquearControles(bloquear: boolean) {
  (document.getElementById("clienteSelect") as HTMLSelectElement).disabled =
    bloquear;
  (document.getElementById("puntoVentaSelect") as HTMLSelectElement).disabled =
    bloquear;
  (document.getElementById("monedaSelect") as HTMLSelectElement).disabled =
    bloquear;
  (document.getElementById("numeroFacturaInput") as HTMLInputElement).disabled =
    bloquear;
  (document.getElementById("btnAgregarLinea") as HTMLButtonElement).disabled =
    bloquear;
}

function resetearAModoNuevo() {
  facturaCargadaActual = null;
  detallesActuales = [];
  (document.getElementById("buscarIdInput") as HTMLInputElement).value = "";
  (document.getElementById("numeroFacturaInput") as HTMLInputElement).value =
    "";
  (document.getElementById("clienteSelect") as HTMLSelectElement).value = "";
  (document.getElementById("puntoVentaSelect") as HTMLSelectElement).value = "";
  (document.getElementById("monedaSelect") as HTMLSelectElement).value = "";

  document.getElementById("alertaEstado")!.style.display = "none";
  document.getElementById("btnGuardarFactura")!.style.display = "inline-block";
  document.getElementById("btnActualizarFactura")!.style.display = "none";
  document.getElementById("btnAnularFactura")!.style.display = "none";
  document.getElementById("btnEliminarFactura")!.style.display = "none";

  bloquearControles(false);
  actualizarTablaUI();
}

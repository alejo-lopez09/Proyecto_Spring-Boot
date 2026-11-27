const app = document.querySelector("#app");
const toast = document.querySelector("#toast");
const state = {
  activePage: "inicio",
  locations: [],
  rooms: [],
  services: [],
  plans: [],
  members: [],
  reservations: [],
  availableRooms: [],
};

const pageInfo = {
  inicio: { title: "Inicio", label: "TU ESPACIO DE TRABAJO" },
  disponibilidad: { title: "Encuentra tu espacio", label: "DISPONIBILIDAD" },
  reservas: { title: "Tus reservas", label: "ORGANIZA TU TIEMPO" },
  catalogo: { title: "Explora CoworkHub", label: "NUESTROS ESPACIOS" },
};

const localDate = (date) => {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
};
const today = () => localDate(new Date());
const tomorrow = () => {
  const date = new Date();
  date.setDate(date.getDate() + 1);
  return localDate(date);
};
const html = (value) => String(value ?? "").replace(/[&<>"']/g, (char) => ({
  "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;",
}[char]));
const money = (value) => new Intl.NumberFormat("es-AR", {
  style: "currency", currency: "ARS", maximumFractionDigits: 0,
}).format(Number(value || 0));
const dateLabel = (value, options = { day: "numeric", month: "short" }) =>
  new Intl.DateTimeFormat("es", options).format(new Date(value));

function showToast(message, isError = false) {
  toast.textContent = message;
  toast.classList.toggle("error", isError);
  toast.classList.add("show");
  window.clearTimeout(showToast.timeout);
  showToast.timeout = window.setTimeout(() => toast.classList.remove("show"), 3600);
}

async function api(path, options = {}) {
  const headers = new Headers(options.headers || {});
  if (options.body && !(options.body instanceof FormData)) headers.set("Content-Type", "application/json");
  const response = await fetch(path, { ...options, headers });
  if (!response.ok) {
    let message = `No se pudo completar la solicitud (${response.status}).`;
    try {
      const body = await response.json();
      message = body.error || body.message || message;
    } catch { /* Algunos endpoints responden sin contenido. */ }
    throw new Error(message);
  }
  if (response.status === 204 || response.headers.get("content-length") === "0") return null;
  const body = await response.text();
  return body ? JSON.parse(body) : null;
}

async function loadWorkspace() {
  renderShell();
  const commonRequests = [
    api("/api/sedes").then((value) => { state.locations = value; }),
    api("/api/salas").then((value) => { state.rooms = value; }),
    api("/api/servicios-adicionales").then((value) => { state.services = value; }),
    api("/api/planes").then((value) => { state.plans = value; }),
    api("/api/miembros").then((value) => { state.members = value; }),
  ];
  const results = await Promise.allSettled(commonRequests);
  const failed = results.find((result) => result.status === "rejected");
  if (failed) showToast(failed.reason.message, true);
  await loadReservations(today());
  renderPage();
}

function navButton(key, icon, title) {
  return `<button class="nav-link ${state.activePage === key ? "active" : ""}" data-page="${key}">
    <span class="nav-icon" aria-hidden="true">${icon}</span>${title}
  </button>`;
}

function renderShell() {
  app.innerHTML = `
    <div class="app-shell">
      <aside class="sidebar" id="sidebar">
        <a class="brand" href="#" aria-label="CoworkHub, inicio">
          <span class="brand-mark">C</span><span>CoworkHub<small>Tu lugar para crecer</small></span>
        </a>
        <p class="nav-label">Menú principal</p>
        <nav class="nav-list" aria-label="Navegación principal">
          ${navButton("inicio", "⌂", "Inicio")}
          ${navButton("disponibilidad", "⌕", "Buscar espacios")}
          ${navButton("reservas", "▤", "Reservas")}
          ${navButton("catalogo", "◇", "Sedes y servicios")}
        </nav>
        <div class="sidebar-bottom">
          <div class="help-card"><strong>¿Necesitas una mano?</strong><p>Tu equipo de recepción está para ayudarte a encontrar el espacio ideal.</p></div>
        </div>
      </aside>
      <section class="main-area">
        <header class="topbar">
          <div class="topbar-left"><button class="mobile-menu" id="mobile-menu" aria-label="Abrir menú">☰</button><div class="breadcrumb">CoworkHub <span aria-hidden="true">/</span> <strong id="breadcrumb">Inicio</strong></div></div>
          <div class="topbar-right"><span class="online-dot"></span> Tu espacio está listo</div>
        </header>
        <main class="content" id="page-content"></main>
      </section>
    </div>
    <dialog id="booking-dialog"></dialog>`;
  app.querySelectorAll("[data-page]").forEach((button) => button.addEventListener("click", () => navigate(button.dataset.page)));
  document.querySelector("#mobile-menu").addEventListener("click", () => document.querySelector("#sidebar").classList.toggle("open"));
}

async function navigate(page) {
  state.activePage = page;
  document.querySelector("#sidebar").classList.remove("open");
  document.querySelectorAll("[data-page]").forEach((button) => button.classList.toggle("active", button.dataset.page === page));
  document.querySelector("#breadcrumb").textContent = pageInfo[page].title;
  if (page === "reservas") await loadReservations(document.querySelector("#booking-date-filter")?.value || today());
  renderPage();
}

async function loadReservations(date = today()) {
  const params = new URLSearchParams();
  params.set("fecha", date);
  try {
    state.reservations = await api(`/api/reservas?${params}`);
  } catch (error) {
    state.reservations = [];
    showToast(error.message, true);
  }
}

function renderPage() {
  const page = pageInfo[state.activePage] ? state.activePage : "inicio";
  const content = document.querySelector("#page-content");
  if (!content) return;
  if (page === "inicio") content.innerHTML = renderDashboard();
  if (page === "disponibilidad") content.innerHTML = renderAvailability();
  if (page === "reservas") content.innerHTML = renderReservations();
  if (page === "catalogo") content.innerHTML = renderCatalog();
  bindPageEvents();
}

function renderHeading(title, subtitle, action = "") {
  return `<div class="page-heading"><div><p class="eyebrow">${html(pageInfo[state.activePage]?.label)}</p><h1>${title}</h1><p>${subtitle}</p></div>${action}</div>`;
}

function reservationList(items, emptyText = "No hay reservas para mostrar.") {
  if (!items.length) return `<div class="empty-state"><span class="empty-icon">▤</span><strong>Todo despejado</strong><p>${html(emptyText)}</p></div>`;
  return `<div class="table-wrap"><table class="data-table">
    <thead><tr><th>Espacio</th><th>Fecha y hora</th><th>Miembro</th><th>Estado</th><th>Total</th></tr></thead>
    <tbody>${items.map((item) => `<tr>
      <td><span class="table-primary">${html(item.sala)}</span></td>
      <td>${html(dateLabel(item.startTime, { day: "numeric", month: "short" }))} · ${html(String(item.startTime).slice(11, 16))}</td>
      <td>${html(item.miembro)}</td>
      <td><span class="status ${html(String(item.status).toLowerCase())}">${html(statusLabel(item.status))}</span></td>
      <td>${money(item.totalCost)}</td>
    </tr>`).join("")}</tbody>
  </table></div>`;
}

function statusLabel(status) {
  return ({ PENDING: "Pendiente", CONFIRMED: "Confirmada", CANCELLED: "Cancelada", COMPLETED: "Completada" })[status] || status;
}

function renderDashboard() {
  const active = state.reservations.filter((item) => ["PENDING", "CONFIRMED"].includes(item.status));
  const places = new Set(state.rooms.map((room) => room.sedeId)).size;
  const nextBooking = active.slice().sort((a, b) => a.startTime.localeCompare(b.startTime))[0];
  return `${renderHeading("Bienvenido a CoworkHub 👋", "Encuentra el espacio ideal para tu próximo día de trabajo.", `<button class="button" data-go="disponibilidad">＋ Reservar un espacio</button>`)}
    <section class="stats-grid" aria-label="Resumen">
      ${statCard("Reservas activas", active.length, "En curso o por confirmar", "▤")}
      ${statCard("Miembros", state.members.length, "En la comunidad", "♙")}
      ${statCard("Sedes", places, "Espacios para descubrir", "⌖")}
      ${statCard("Planes", state.plans.length, "Opciones para cada ritmo", "◇")}
    </section>
    <section class="dashboard-grid">
      <article class="card">
        <div class="card-heading"><div><h2>${nextBooking ? "Tu próximo encuentro" : "Reservas de hoy"}</h2><p>${nextBooking ? "Un vistazo a tu espacio reservado" : "Lo que sucede en tu espacio hoy"}</p></div><button class="text-button" data-go="reservas">Ver reservas →</button></div>
        ${reservationList(nextBooking ? [nextBooking] : state.reservations)}
      </article>
      <article class="card">
        <div class="card-heading"><div><h2>Nuestras sedes</h2><p>Encuentra tu lugar favorito</p></div></div>
        ${renderLocationList(state.locations)}
        <button class="button secondary small" data-go="catalogo" style="width:100%;margin-top:12px">Explorar sedes</button>
      </article>
    </section>`;
}

function statCard(label, value, note, icon) {
  return `<article class="stat-card"><div class="stat-top"><span>${html(label)}</span><span class="stat-icon">${icon}</span></div><div class="stat-value">${html(value)}</div><div class="stat-note">${html(note)}</div></article>`;
}

function renderLocationList(locations) {
  if (!locations.length) return `<div class="empty-state"><strong>Estamos preparando todo</strong><p>Vuelve a consultar en un momento.</p></div>`;
  return `<div class="location-list">${locations.slice(0, 4).map((location) => `<div class="location-item">
    <span class="location-icon">⌖</span><div><strong>${html(location.name)}</strong><span>${html(location.city)} · ${html(location.openingTime)}–${html(location.closingTime)}</span></div>
  </div>`).join("")}</div>`;
}

function renderAvailability() {
  const locationOptions = state.locations.map((location) => `<option value="${html(location.id)}">${html(location.name)} · ${html(location.city)}</option>`).join("");
  return `${renderHeading("Encuentra tu espacio", "Elige cuándo y dónde quieres trabajar.")}
    <section class="card search-card">
      <div class="card-heading"><div><h2>Cuéntanos qué necesitas</h2><p>Te mostraremos los espacios disponibles en ese horario.</p></div></div>
      <form class="form-grid" id="availability-form">
        <div class="form-field"><label for="availability-location">Sede</label><select id="availability-location" name="sedeId" required>${locationOptions}</select></div>
        <div class="form-field"><label for="availability-date">Fecha</label><input id="availability-date" name="fecha" type="date" min="${tomorrow()}" value="${tomorrow()}" required></div>
        <div class="form-field"><label for="availability-start">Desde</label><input id="availability-start" name="horaInicio" type="time" value="10:00" required></div>
        <div class="form-field"><label for="availability-end">Hasta</label><input id="availability-end" name="horaFin" type="time" value="11:00" required></div>
        <div class="form-field"><label>&nbsp;</label><button class="button" type="submit">Buscar espacios <span aria-hidden="true">→</span></button></div>
      </form>
    </section>
    <section><div class="card-heading"><div><h2>Espacios disponibles</h2><p id="availability-caption">Completa la búsqueda para ver opciones a tu medida.</p></div></div>
      <div id="availability-results" class="results-grid"></div>
    </section>`;
}

function renderRoomCards(rooms, search) {
  if (!rooms.length) return `<div class="card empty-state" style="grid-column:1/-1"><span class="empty-icon">⌕</span><strong>No encontramos espacios libres</strong><p>Prueba con otro horario o una sede diferente.</p></div>`;
  return rooms.map((room) => `<article class="room-card">
    <span class="room-kicker">${html(room.type)}</span><h3>${html(room.sala)}</h3><p>${html(room.sede)}</p>
    <div class="room-facts"><span>♙ Hasta ${html(room.capacity)} personas</span><span>⌖ ${html(room.equipamiento?.length || 0)} servicios</span></div>
    <div class="room-price"><strong>${money(room.hourlyRate)}</strong><span>/ hora</span></div>
    <button class="button small" data-book-room="${html(room.salaId)}" data-date="${html(search.fecha)}" data-start="${html(search.horaInicio)}" data-end="${html(search.horaFin)}">Elegir este espacio →</button>
  </article>`).join("");
}

function renderReservations() {
  const filter = `<form class="filter-row" id="booking-filter-form">
      <div class="form-field"><label for="booking-date-filter">Filtrar por fecha</label><input id="booking-date-filter" type="date" value="${today()}"></div>
      <button class="button secondary" type="submit">Buscar</button>
    </form>`;
  const actions = state.reservations.map((item) => {
    const buttons = [];
    if (item.status === "PENDING") buttons.push(`<button class="text-button" data-action="confirmar" data-id="${html(item.id)}">Confirmar</button>`);
    if (["PENDING", "CONFIRMED"].includes(item.status)) buttons.push(`<button class="text-button danger" data-action="cancelar" data-id="${html(item.id)}">Cancelar</button>`);
    if (item.status === "CONFIRMED") buttons.push(`<button class="text-button" data-action="completar" data-id="${html(item.id)}">Completar</button>`);
    return `<tr><td><span class="table-primary">${html(item.sala)}</span></td><td>${html(dateLabel(item.startTime, { day: "numeric", month: "short", year: "numeric" }))}</td><td>${html(String(item.startTime).slice(11, 16))}–${html(String(item.endTime).slice(11, 16))}</td><td>${html(item.miembro)}</td><td><span class="status ${html(String(item.status).toLowerCase())}">${html(statusLabel(item.status))}</span></td><td>${money(item.totalCost)}</td><td><div class="reservation-actions">${buttons.join("") || "—"}</div></td></tr>`;
  }).join("");
  const table = state.reservations.length ? `<div class="table-wrap"><table class="data-table">
    <thead><tr><th>Espacio</th><th>Fecha</th><th>Horario</th><th>Miembro</th><th>Estado</th><th>Total</th><th>Acciones</th></tr></thead><tbody>${actions}</tbody></table></div>`
    : `<div class="empty-state"><span class="empty-icon">▤</span><strong>Aún no tienes reservas</strong><p>Cuando elijas tu primer espacio, lo verás aquí.</p><button class="button small" data-go="disponibilidad" style="margin-top:15px">Buscar espacios</button></div>`;
  return `${renderHeading("Reservas", "Consulta las reservas del día y gestiona sus estados.", `<button class="button" data-go="disponibilidad">＋ Nueva reserva</button>`)}
    <section class="card"><div class="card-heading"><div><h2>Agenda</h2><p>Reservas según la fecha seleccionada</p></div>${filter}</div>${table}</section>`;
}

function renderCatalog() {
  const locations = state.locations.map((location) => `<article class="catalog-item"><span class="location-icon">⌖</span><div><strong>${html(location.name)}</strong><p>${html(location.city)} · ${html(location.address)}<br>${html(location.openingTime)}–${html(location.closingTime)}</p></div></article>`).join("");
  const rooms = state.rooms.map((room) => `<article class="catalog-item"><span class="location-icon">▦</span><div><strong>${html(room.name)}</strong><p>${html(room.sede)} · ${html(room.type)} · ${html(room.capacity)} personas<br>${money(room.hourlyRate)} por hora · ${room.active ? "Disponible para reservar" : "No disponible"}</p></div></article>`).join("");
  const plans = state.plans.map((plan) => `<article class="catalog-item"><span class="location-icon">◇</span><div><strong>${html(plan.name)}</strong><p>${html(plan.includedHoursPerMonth)} horas al mes · hasta ${html(plan.maxActiveBookings)} reservas activas</p></div></article>`).join("");
  const services = state.services.map((service) => `<article class="catalog-item"><span class="location-icon">✦</span><div><strong>${html(service.name)}</strong><p>${money(service.unitPrice)} por unidad</p></div></article>`).join("");
  const section = (title, items, empty) => `<section class="catalog-section"><h2>${title}</h2><div class="catalog-grid">${items || `<div class="empty-state" style="grid-column:1/-1"><p>${empty}</p></div>`}</div></section>`;
  return `${renderHeading("Explora CoworkHub", "Todo lo que necesitas para encontrar tu próximo lugar favorito.")}
    ${section("Nuestras sedes", locations, "No hay sedes disponibles todavía.")}
    ${section("Espacios", rooms, "No hay salas disponibles todavía.")}
    ${section("Planes para ti", plans, "No hay planes publicados todavía.")}
    ${section("Servicios adicionales", services, "No hay servicios publicados todavía.")}`;
}

function bindPageEvents() {
  document.querySelectorAll("[data-go]").forEach((button) => button.addEventListener("click", () => navigate(button.dataset.go)));
  const searchForm = document.querySelector("#availability-form");
  if (searchForm) searchForm.addEventListener("submit", searchAvailability);
  const filterForm = document.querySelector("#booking-filter-form");
  if (filterForm) filterForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    await loadReservations(document.querySelector("#booking-date-filter").value);
    renderPage();
  });
  document.querySelectorAll("[data-action]").forEach((button) => button.addEventListener("click", () => changeBookingStatus(button.dataset.id, button.dataset.action)));
  document.querySelectorAll("[data-book-room]").forEach((button) => button.addEventListener("click", () => openBookingDialog(button.dataset)));
}

async function searchAvailability(event) {
  event.preventDefault();
  const form = new FormData(event.currentTarget);
  const search = Object.fromEntries(form.entries());
  if (search.horaInicio >= search.horaFin) {
    showToast("La hora de fin debe ser posterior a la hora de inicio.", true);
    return;
  }
  const results = document.querySelector("#availability-results");
  const caption = document.querySelector("#availability-caption");
  results.innerHTML = `<div class="empty-state" style="grid-column:1/-1"><strong>Buscando espacios...</strong><p>Un momento, estamos revisando disponibilidad.</p></div>`;
  const params = new URLSearchParams(search);
  try {
    state.availableRooms = await api(`/api/disponibilidad?${params}`);
    caption.textContent = `${dateLabel(`${search.fecha}T12:00:00`, { day: "numeric", month: "long" })} · ${search.horaInicio} a ${search.horaFin}`;
    results.innerHTML = renderRoomCards(state.availableRooms, search);
    bindPageEvents();
  } catch (error) {
    results.innerHTML = `<div class="inline-error" style="grid-column:1/-1">${html(error.message)}</div>`;
  }
}

function openBookingDialog(data) {
  const room = state.availableRooms.find((item) => String(item.salaId) === String(data.bookRoom));
  if (!room) return;
  const activeMembers = state.members.filter((member) => member.status === "ACTIVE");
  const memberOptions = `<div class="form-field full"><label for="booking-member">Miembro</label><select name="miembroId" id="booking-member" required>${activeMembers.map((member) => `<option value="${html(member.id)}">${html(member.fullName)}</option>`).join("")}</select></div>`;
  const services = state.services.length ? `<div class="form-field full"><label>Servicios adicionales (opcional)</label><div class="service-options">${state.services.map((service) => `<div class="service-option"><label><input type="checkbox" name="service-${html(service.id)}" value="${html(service.id)}"> ${html(service.name)}</label><span>${money(service.unitPrice)}</span></div>`).join("")}</div></div>` : "";
  const dialog = document.querySelector("#booking-dialog");
  dialog.innerHTML = `<form class="dialog-content" id="create-booking-form">
    <div class="dialog-header"><div><h2>Reserva tu espacio</h2><p>${html(room.sala)} · ${html(room.sede)}</p></div><button class="dialog-close" type="button" aria-label="Cerrar">×</button></div>
    <div class="form-grid">
      <div class="form-field"><label>Fecha</label><input value="${html(data.date)}" readonly></div>
      <div class="form-field"><label>Horario</label><input value="${html(data.start)} – ${html(data.end)}" readonly></div>
      <div class="form-field"><label for="booking-attendees">Cantidad de personas</label><input id="booking-attendees" name="attendees" type="number" min="1" max="${html(room.capacity)}" value="1" required></div>
      ${memberOptions}${services}
    </div>
    <div class="dialog-footer"><button class="button secondary" type="button" data-close-dialog>Volver</button><button class="button" type="submit">Confirmar reserva</button></div>
  </form>`;
  dialog.showModal();
  dialog.querySelector(".dialog-close").addEventListener("click", () => dialog.close());
  dialog.querySelector("[data-close-dialog]").addEventListener("click", () => dialog.close());
  dialog.querySelector("#create-booking-form").addEventListener("submit", (event) => createBooking(event, room, data));
}

async function createBooking(event, room, data) {
  event.preventDefault();
  const form = new FormData(event.currentTarget);
  const serviceItems = state.services.filter((service) => form.has(`service-${service.id}`)).map((service) => ({ servicioId: service.id, quantity: 1 }));
  const request = {
    salaId: room.salaId,
    miembroId: Number(form.get("miembroId")),
    startTime: `${data.date}T${data.start}`,
    endTime: `${data.date}T${data.end}`,
    attendees: Number(form.get("attendees")),
    servicios: serviceItems,
  };
  try {
    await api("/api/reservas", { method: "POST", body: JSON.stringify(request) });
    document.querySelector("#booking-dialog").close();
    showToast("¡Listo! Tu espacio quedó reservado.");
    await loadReservations(data.date);
    if (state.activePage === "reservas" || state.activePage === "inicio") renderPage();
  } catch (error) { showToast(error.message, true); }
}

async function changeBookingStatus(id, action) {
  const confirmation = action === "cancelar" ? "¿Quieres cancelar esta reserva?" : `¿Quieres ${action} esta reserva?`;
  if (!window.confirm(confirmation)) return;
  try {
    await api(`/api/reservas/${id}/${action}`, { method: "POST" });
    showToast(`Reserva ${action === "confirmar" ? "confirmada" : action === "cancelar" ? "cancelada" : "completada"} correctamente.`);
    await loadReservations(document.querySelector("#booking-date-filter")?.value || today());
    renderPage();
  } catch (error) { showToast(error.message, true); }
}

async function start() {
  try {
    await loadWorkspace();
  } catch (error) {
    app.innerHTML = `<main class="loading-screen"><div class="inline-error">${html(error.message)}</div></main>`;
  }
}

start();

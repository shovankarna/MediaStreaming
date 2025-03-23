const viewerContainer = document.getElementById("viewerContainer");
const zoomDisplay = document.getElementById("zoom-display");
const pageDisplay = document.getElementById("page-display");
const pageInput = document.getElementById("pageInput");
const scrollToTopBtn = document.getElementById("scrollToTopBtn");

let pdfDoc = null;
let zoom = 1.5;
let isRendering = false;

const MIN_ZOOM = 0.5;
const MAX_ZOOM = 3.0;

function updateZoomDisplay() {
  zoomDisplay.textContent = `Zoom: ${Math.round(zoom * 100)}%`;
}

function updatePageDisplay(current, total) {
  pageDisplay.textContent = `Page: ${current} / ${total}`;
  pageInput.max = total;
}

function goToPage(pageNum) {
  const num = parseInt(pageNum);
  if (!num || num < 1 || num > pdfDoc.numPages) return;
  const container = viewerContainer.querySelector(
    `.pdf-page-container[data-page="${num}"]`
  );
  if (container) {
    container.scrollIntoView({ behavior: "smooth", block: "start" });
  }
}

function zoomIn() {
  if (zoom >= MAX_ZOOM) return;
  zoom += 0.25;
  reloadPages();
}

function zoomOut() {
  if (zoom <= MIN_ZOOM) return;
  zoom -= 0.25;
  reloadPages();
}

function reloadPages() {
  const currentScroll = viewerContainer.scrollTop;
  viewerContainer.innerHTML = "";
  for (let pageNum = 1; pageNum <= pdfDoc.numPages; pageNum++) {
    createPagePlaceholder(pageNum);
  }
  updateZoomDisplay();
  observePages();
  viewerContainer.scrollTop = currentScroll;
}

function createPagePlaceholder(pageNum) {
  const container = document.createElement("div");
  container.className = "pdf-page-container";
  container.setAttribute("data-page", pageNum);

  //   const spinner = document.createElement("div");
  //   spinner.className = "spinner";

  //   container.appendChild(spinner);
  viewerContainer.appendChild(container);
}

// Lazy-load visible pages
const pageObserver = new IntersectionObserver(
  (entries) => {
    entries.forEach((entry) => {
      if (entry.isIntersecting && !entry.target.dataset.rendered) {
        const pageNum = parseInt(entry.target.dataset.page);
        renderPage(pageNum, entry.target);
      }
    });
  },
  { root: viewerContainer, rootMargin: "50px 0px", threshold: 0.5 }
);

function observePages() {
  document.querySelectorAll(".pdf-page-container").forEach((container) => {
    pageObserver.observe(container);
  });
}

function renderPage(pageNum, container) {
  if (isRendering) return; // prevent multiple simultaneous renders
  isRendering = true;

  pdfDoc.getPage(pageNum).then((page) => {
    const viewport = page.getViewport({ scale: zoom });
    const canvas = document.createElement("canvas");
    canvas.className = "pdf-page";
    canvas.height = viewport.height;
    canvas.width = viewport.width;
    const context = canvas.getContext("2d");

    // Show spinner while rendering
    container.innerHTML = "";
    const spinner = document.createElement("div");
    spinner.className = "spinner";
    container.appendChild(spinner);

    // Render page
    page.render({ canvasContext: context, viewport }).promise.then(() => {
      container.innerHTML = ""; // Remove spinner
      canvas.classList.add("pdf-page-loaded"); // ✅ Apply fade-in
      container.appendChild(canvas);
      container.dataset.rendered = "true";
      isRendering = false;
    });
  });
}

function toggleFullscreen() {
  const elem = document.documentElement;
  const icon = document.getElementById("fullscreen-icon");

  if (!document.fullscreenElement) {
    elem
      .requestFullscreen()
      .then(() => {
        icon.classList.remove("fa-expand");
        icon.classList.add("fa-compress");
      })
      .catch((err) => {
        console.warn(`Error attempting to enable fullscreen: ${err.message}`);
      });
  } else {
    document.exitFullscreen().then(() => {
      icon.classList.remove("fa-compress");
      icon.classList.add("fa-expand");
    });
  }
}

function toggleNightMode() {
  const icon = document.getElementById("night-mode-icon");
  document.body.classList.toggle("night-mode");

  if (document.body.classList.contains("night-mode")) {
    icon.classList.remove("fa-moon");
    icon.classList.add("fa-sun");
  } else {
    icon.classList.remove("fa-sun");
    icon.classList.add("fa-moon");
  }
}

// Track visible page number
const pageTracker = new IntersectionObserver(
  (entries) => {
    entries.forEach((entry) => {
      if (entry.isIntersecting) {
        const visiblePage = parseInt(entry.target.getAttribute("data-page"));
        updatePageDisplay(visiblePage, pdfDoc.numPages);
      }
    });
  },
  { root: viewerContainer, threshold: 0.5 }
);

// Load PDF
pdfjsLib
  .getDocument(pdfUrl)
  .promise.then((pdf) => {
    pdfDoc = pdf;
    updateZoomDisplay();

    for (let i = 1; i <= pdf.numPages; i++) {
      createPagePlaceholder(i);
    }

    observePages();

    // Page tracking
    document
      .querySelectorAll(".pdf-page-container")
      .forEach((c) => pageTracker.observe(c));
  })
  .catch((err) => {
    viewerContainer.innerHTML = `<p style="padding:20px;color:red;">Failed to load PDF: ${err.message}</p>`;
  });

// Show/hide button based on scroll position
viewerContainer.addEventListener("scroll", () => {
  if (viewerContainer.scrollTop > 300) {
    scrollToTopBtn.style.display = "block";
  } else {
    scrollToTopBtn.style.display = "none";
  }
});

// Scroll to top behavior
function scrollToTop() {
  viewerContainer.scrollTo({ top: 0, behavior: "smooth" });
}

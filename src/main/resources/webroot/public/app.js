/* app.js – Front-end helper for the demo page */

async function sayHello() {
    const name = document.getElementById('nameInput').value.trim();
    if (!name) { alert('Please enter a name.'); return; }

    try {
        const response = await fetch('/App/hello?name=' + encodeURIComponent(name));
        const text = await response.text();
        document.getElementById('result').textContent = text;
    } catch (err) {
        document.getElementById('result').textContent = 'Error: ' + err.message;
    }
}

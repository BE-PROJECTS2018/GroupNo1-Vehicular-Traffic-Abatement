const Pageres = require('pageres');

var count = 0;

function takeSS(n, resolve) {
    const pageres = new Pageres({delay: 5})
    .src('localhost:9000?north=19.06601179781123&east=72.9323959350586&south=19.03761657578407&west=72.8737735748291&zoom=15', ['1024x768'], {crop: true, filename: "<%= date %> - <%= time %>"})
    .dest("images")
    .run()
    .then(() => {
        count++;
        n--;
        if(n == 0) {
            resolve();
        } else {
            takeSS(n, resolve);
        }
    });
}

new Promise((resolve, reject) => {
    takeSS(2, resolve);    
}).then(()=>console.log("Taken " + count + " screenshots."))

const Pageres = require('pageres');

var count = 0;

function takeSS(n, resolve) {
    const d = new Date();
    const fname = d.getDay() + "-" + d.getHours() + "-" + d.getMinutes();
    const timeout = 15; //mins
    const pageres = new Pageres({delay: 10})
    .src('localhost:9000?north=19.062605&east=72.920251&south=19.041024&west=72.885919&zoom=15', ['800x600'], {crop: true, filename: fname})
    .dest("images/section1")
    .run()
    .then(() => {
        count++;
        n--;
        if(n == 0) {
            resolve();
        } else {
            setTimeout(takeSS(n, resolve), timeout*60*1000);
        }
    });
}

var n;
try {
    n = Number(process.argv.find((v)=>v.match('-n=')).substring(3));
} catch(e) {
    console.error("Enter a valid count as -n=<count> command line option");
    process.exit();
}

new Promise((resolve, reject) => {
    takeSS(n, resolve);    
}).then(()=>console.log("Taken " + count + " screenshots."))

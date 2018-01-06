const Pageres = require('pageres');

var count = 0;
const timeout = 15; //mins
function takeSS(n, resolve) {
    const d = new Date();
    const fname = d.getDay() + "-" + d.getHours() + "-" + d.getMinutes();
    const pageres = new Pageres({delay: 10})
    //NE (19.06398378548459, 72.92025113769535), SW (19.039645023697126, 72.88591886230472)
    .src('localhost:9000?north=19.062605&east=72.920251&south=19.041024&west=72.885919&zoom=15', ['800x600'], {crop: true, filename: '<%= date %> '+fname})
    .dest("images/section1")
    .run()
    .then(() => {
        count++;
        n--;
        if(n == 0) {
            resolve();
        } else {
            setTimeout(() => {
                takeSS(n, resolve)
            }, timeout*60*1000);
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

setTimeout(() => {
    new Promise((resolve, reject) => {
        takeSS(n, resolve)  
    }).then(()=>console.log("Taken " + count + " screenshots."));
}, 10*1000);

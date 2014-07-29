let rec gcd m n = 
    if m = 0 then 
        n 
    else if m <= n then 
        gcd m (n-m) 
    else 
        gcd n (m-n) 
;;


let rec fib m = 
    if m = 1 then 
        1 
    else if m = 2 then 
        1 
    else ( fib (m-1) ) + ( fib (m-2) )
;;

let rec loop n =
    if n <> 0 then loop (n-1)
    else 0
in
    loop 5 
end ;;
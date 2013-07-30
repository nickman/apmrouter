/**
 * Script to test Virtual Agent State Changes.
 * Switches between periods of metric submissions to periods of inactivity.
 * In the background, a thread checks that the virtual agent state is as expected.
 * INIT ->  One time state at start
 * UP -> Virtual Agent and Virtual Tracer are active
 * SOFTDOWN -> Virtual Agent and Virtual Tracer are inactive and pending HARDDOWN unless activated to UP
 * HARDDOWN -> Virtual Agent and Virtual Tracer are marked totally inactive
 * Whitehead, Helios Dev
 */

